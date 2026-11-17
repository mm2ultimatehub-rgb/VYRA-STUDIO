package studio.vyra.app.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the latest stable VYRA GitHub release.
 *
 * This class only discovers and validates release metadata. It deliberately does not
 * install APKs: Android's PackageInstaller must obtain explicit user confirmation.
 */
data class AppUpdate(
    val version: String,
    val releaseNotes: String,
    val apkUrl: String,
    val sha256: String?,
    val fileSizeBytes: Long,
    val releasePageUrl: String,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val update: AppUpdate) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val reason: String) : UpdateCheckResult
}

class AppUpdateChecker(
    private val owner: String = "mm2ultimatehub-rgb",
    private val repository: String = "VYRA-STUDIO",
) {
    fun check(currentVersion: String): UpdateCheckResult {
        val connection = try {
            URL("https://api.github.com/repos/$owner/$repository/releases/latest")
                .openConnection() as HttpURLConnection
        } catch (error: Exception) {
            return UpdateCheckResult.Failed(error.message ?: "Could not connect to GitHub")
        }

        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "VYRA-STUDIO-Android")
            connection.requestMethod = "GET"

            if (connection.responseCode !in 200..299) {
                return UpdateCheckResult.Failed("GitHub returned HTTP ${connection.responseCode}")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(body)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                return UpdateCheckResult.Failed("Latest release is not a stable public release")
            }

            val version = release.optString("tag_name")
                .removePrefix("v")
                .takeIf { it.isNotBlank() }
                ?: return UpdateCheckResult.Failed("Release has no version tag")

            if (compareVersions(version, currentVersion) <= 0) {
                return UpdateCheckResult.UpToDate
            }

            val assets = release.optJSONArray("assets")
                ?: return UpdateCheckResult.Failed("Release has no APK assets")

            var apk: JSONObject? = null
            for (index in 0 until assets.length()) {
                val candidate = assets.optJSONObject(index) ?: continue
                val name = candidate.optString("name")
                if (name.endsWith(".apk", ignoreCase = true) &&
                    (name.equals("vyra-studio.apk", true) ||
                        name.equals("app-release.apk", true) ||
                        name.startsWith("vyra-studio-", true))
                ) {
                    apk = candidate
                    break
                }
            }

            val selected = apk ?: return UpdateCheckResult.Failed(
                "Stable release exists, but no recognized VYRA APK asset was found"
            )
            val downloadUrl = selected.optString("browser_download_url")
            val downloadUri = runCatching { URL(downloadUrl).toURI() }.getOrNull()
                ?: return UpdateCheckResult.Failed("APK download URL is invalid")
            if (downloadUri.scheme != "https" ||
                downloadUri.host !in setOf("github.com", "objects.githubusercontent.com")
            ) {
                return UpdateCheckResult.Failed("APK download host is not trusted")
            }

            val digest = selected.optString("digest")
                .takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:")
                ?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }

            UpdateCheckResult.UpdateAvailable(
                AppUpdate(
                    version = version,
                    releaseNotes = release.optString("body"),
                    apkUrl = downloadUrl,
                    sha256 = digest,
                    fileSizeBytes = selected.optLong("size", 0L),
                    releasePageUrl = release.optString("html_url"),
                )
            )
        } catch (error: Exception) {
            UpdateCheckResult.Failed(error.message ?: "Could not read release metadata")
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
        val b = right.removePrefix("v").split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
