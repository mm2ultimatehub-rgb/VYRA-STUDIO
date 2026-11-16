package studio.vyra.app.repository

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

class RepositoryClient(
    private val parser: RepositoryIndexParser = RepositoryIndexParser(),
) {
    fun fetch(indexUrl: String): PatchRepository {
        val uri = validateUrl(indexUrl)
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VyraStudio/0.1")
        }

        try {
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) {
                if (status in 300..399) "Repository redirects are not accepted. Use the final HTTPS URL."
                else "Repository request failed with HTTP $status."
            }
            val contentType = connection.contentType.orEmpty()
            require(contentType.isBlank() || contentType.contains("json", ignoreCase = true) ||
                contentType.startsWith("text/plain", ignoreCase = true)) {
                "Repository response is not JSON."
            }
            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= MAX_INDEX_BYTES) {
                "Repository index is larger than the 1 MB limit."
            }

            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_INDEX_BYTES) {
                        "Repository index is larger than the 1 MB limit."
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = String(bytes, StandardCharsets.UTF_8)
            return parser.parse(json).copy(sourceUrl = uri.toString())
        } finally {
            connection.disconnect()
        }
    }

    private fun validateUrl(value: String): URI {
        require(value.length <= MAX_URL_LENGTH) { "Repository URL is too long." }
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { throw IllegalArgumentException("Enter a valid repository URL.", it) }
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Repository indexes must use HTTPS."
        }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) {
            "Repository URL must have a host and cannot contain credentials or a fragment."
        }
        return uri
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_INDEX_BYTES = 1_048_576
        const val MAX_URL_LENGTH = 2_048
    }
}
