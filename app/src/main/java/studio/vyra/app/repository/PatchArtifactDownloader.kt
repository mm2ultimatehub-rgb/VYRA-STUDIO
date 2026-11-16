package studio.vyra.app.repository

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

class PatchArtifactDownloader {
    fun downloadAndVerify(patch: PatchEntry, cacheDirectory: File): File {
        val uri = validateUrl(patch.artifactUrl)
        require(patch.format == "mpp") { "Unsupported patch artifact format." }
        require(SHA256_PATTERN.matches(patch.artifactSha256)) { "Patch SHA-256 is invalid." }

        val directory = File(cacheDirectory, "verified-patches")
        require(directory.exists() || directory.mkdirs()) { "Could not prepare patch cache." }
        val target = File(directory, "${patch.id}-${patch.artifactSha256.take(12)}.mpp")
        if (target.isFile && sha256(target) == patch.artifactSha256.lowercase()) return target
        if (target.exists()) target.delete()

        val temporary = File.createTempFile("vyra-", ".part", directory)
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/octet-stream, application/x-mpp, */*")
            setRequestProperty("User-Agent", "VyraStudio/0.1")
        }

        try {
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) {
                if (status in 300..399) "Artifact redirects are not accepted. Use the final HTTPS URL."
                else "Artifact download failed with HTTP $status."
            }
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
            connection.disconnect()
        }
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
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
    }
}
