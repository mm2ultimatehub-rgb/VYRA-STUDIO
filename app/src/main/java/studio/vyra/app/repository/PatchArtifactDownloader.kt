package studio.vyra.app.repository

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

class PatchArtifactDownloader {
    fun downloadAndVerify(patch: PatchEntry, cacheDirectory: File): File {
        val initialUri = validateUrl(patch.artifactUrl)
        require(patch.format == "mpp") { "Unsupported patch artifact format." }
        require(SHA256_PATTERN.matches(patch.artifactSha256)) { "Patch SHA-256 is invalid." }
        require(PATCH_ID_PATTERN.matches(patch.id)) { "Patch id has an invalid format." }

        val directory = File(cacheDirectory, "verified-patches")
        require(directory.exists() || directory.mkdirs()) { "Could not prepare patch cache." }
        val target = File(directory, "${patch.id}-${patch.artifactSha256.take(12)}.mpp")
        if (target.isFile && sha256(target) == patch.artifactSha256.lowercase()) return target
        if (target.exists()) target.delete()

        val temporary = File.createTempFile("vyra-", ".part", directory)
        var connection: HttpURLConnection? = null
        try {
            connection = openFinalConnection(initialUri)
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) { "Artifact download failed with HTTP $status." }
            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= MAX_ARTIFACT_BYTES) {
                "Patch artifact is larger than the 50 MB limit."
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_ARTIFACT_BYTES) {
                            "Patch artifact is larger than the 50 MB limit."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }

            val actualDigest = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualDigest.equals(patch.artifactSha256, ignoreCase = true)) {
                "Patch integrity check failed. The downloaded file was discarded."
            }
            require(temporary.renameTo(target)) { "Could not save the verified patch artifact." }
            return target
        } catch (error: Exception) {
            temporary.delete()
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    private fun openFinalConnection(start: URI): HttpURLConnection {
        var current = start
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val connection = (URL(current.toString()).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/octet-stream, application/x-mpp, */*")
                setRequestProperty("User-Agent", "VyraStudio/0.1")
            }
            val status = connection.responseCode
            if (status !in 300..399) return connection
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            require(redirectCount < MAX_REDIRECTS) { "Artifact redirected too many times." }
            require(!location.isNullOrBlank()) { "Artifact redirect is missing its destination." }
            val next = runCatching { current.resolve(location) }
                .getOrElse { throw IllegalArgumentException("Artifact redirect URL is invalid.", it) }
            current = validateUrl(next.toString())
            require(current.host.lowercase() in REDIRECT_HOSTS) {
                "Artifact redirect points to an unapproved host: ${current.host}"
            }
        }
        throw IllegalArgumentException("Artifact redirect limit exceeded.")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun validateUrl(value: String): URI {
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { throw IllegalArgumentException("Patch artifact URL is invalid.", it) }
        require(uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) {
            "Patch artifacts must use a valid HTTPS URL without credentials or fragments."
        }
        return uri
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_ARTIFACT_BYTES = 50L * 1024L * 1024L
        const val MAX_REDIRECTS = 5
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
        val PATCH_ID_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{1,159}")
        val REDIRECT_HOSTS = setOf(
            "github.com",
            "release-assets.githubusercontent.com",
            "objects.githubusercontent.com",
            "github-releases.githubusercontent.com",
        )
    }
}
