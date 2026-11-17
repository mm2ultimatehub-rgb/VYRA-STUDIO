/*
 * VYRA catalog bridge for the pinned Morphe Manager runtime.
 * Catalog metadata is untrusted input. Only explicitly verified and enabled
 * official sources may be imported into the executable patch repository.
 */
package app.morphe.manager.domain.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

class VyraCatalogRepository(
    private val importer: VyraLocalBundleImporter
) {
    data class Candidate(
        val appId: String,
        val appName: String,
        val packageName: String,
        val appStatus: String,
        val patchStatus: String,
        val validationStatus: String,
        val sourceListedForApp: Boolean,
        val sourceId: String,
        val publisher: String,
        val repository: String,
        val trust: String,
        val execution: String,
        val releaseVersion: String,
        val artifactUrl: String,
        val sha256: String,
        val targetVersions: Set<String>
    ) {
        val canImport: Boolean
            get() = appStatus == "verified" &&
                patchStatus == "verified" &&
                validationStatus == "passed" &&
                sourceListedForApp &&
                sourceId == "morphe-official" &&
                releaseVersion == TRUSTED_OFFICIAL_RELEASE &&
                sha256 == TRUSTED_OFFICIAL_SHA256 &&
                publisher == "MorpheApp" &&
                repository == "https://github.com/MorpheApp/morphe-patches" &&
                artifactUrl == TRUSTED_OFFICIAL_ARTIFACT_URL &&
                trust == "official-publisher" &&
                execution == "enabled" &&
                targetVersions.isNotEmpty()
    }

    suspend fun findCompatible(packageName: String, versionName: String): List<Candidate> =
        withContext(Dispatchers.IO) {
            val root = JSONObject(getJson(ROOT_URL))
            require(root.optInt("schemaVersion", -1) == 1) { "Unsupported VYRA catalog schema." }
            val catalog = root.getJSONObject("catalog")
            val appsIndexUrl = checkedCatalogUrl(catalog.getString("apps"))
            val sourcesIndexUrl = checkedCatalogUrl(catalog.getString("patchSources"))
            val appIndex = JSONObject(getJson(appsIndexUrl.toString()))
            val sourceIndex = JSONObject(getJson(sourcesIndexUrl.toString()))
            require(appIndex.optInt("schemaVersion", -1) == 1) { "Unsupported VYRA app index." }
            require(sourceIndex.optInt("schemaVersion", -1) == 1) { "Unsupported VYRA source index." }

            val sources = sourceIndex.getJSONArray("sources").associateByString("id")
            val apps = appIndex.getJSONArray("apps")
            buildList {
                for (i in 0 until apps.length()) {
                    val app = apps.getJSONObject(i)
                    if (app.optString("packageName") != packageName) continue
                    val recordName = app.optString("appRecord")
                    require(RECORD_PATTERN.matches(recordName)) { "Invalid VYRA app record path." }
                    val recordUrl = appsIndexUrl.toString().substringBeforeLast('/') + "/" + recordName
                    val record = JSONObject(getJson(checkedCatalogUrl(recordUrl).toString()))
                    require(record.optInt("schemaVersion", -1) == 1) { "Unsupported VYRA app record." }
                    require(record.optString("id") == app.optString("id") &&
                        record.optString("packageName") == packageName) {
                        "VYRA app record identity mismatch."
                    }
                    val allowedSourceIds = app.optJSONArray("patchSourceIds").stringValues().toSet()
                    val validationStatus = record.optJSONObject("validation")?.optString("status", "not-tested") ?: "not-tested"
                    val patchSources = record.optJSONArray("patchSources") ?: JSONArray()
                    for (j in 0 until patchSources.length()) {
                        val patch = patchSources.getJSONObject(j)
                        val versions = patch.optJSONArray("targetVersions").stringValues()
                        if (versionName !in versions) continue
                        val sourceId = patch.optString("sourceId")
                        if (sourceId !in allowedSourceIds) continue
                        val source = sources[sourceId] ?: continue
                        val release = source.optJSONObject("latestRelease") ?: continue
                        val artifactUrl = release.optString("assetUrl")
                        val digest = release.optString("sha256").lowercase()
                        require(SHA256_PATTERN.matches(digest)) { "VYRA source has an invalid SHA-256." }
                        validateArtifactUrl(artifactUrl)
                        add(
                            Candidate(
                                appId = app.optString("id"),
                                appName = app.optString("name"),
                                packageName = packageName,
                                appStatus = app.optString("status", "unsupported"),
                                patchStatus = patch.optString("status", "unsupported"),
                                validationStatus = validationStatus,
                                sourceListedForApp = sourceId in allowedSourceIds,
                                sourceId = sourceId,
                                publisher = source.optString("publisher"),
                                repository = source.optString("repository"),
                                trust = source.optString("trust", "unverified"),
                                execution = source.optString("execution", "disabled"),
                                releaseVersion = release.optString("version"),
                                artifactUrl = artifactUrl,
                                sha256 = digest,
                                targetVersions = versions.toSet()
                            )
                        )
                    }
                }
            }
        }

    suspend fun downloadAndImport(candidate: Candidate, cacheDirectory: File): VyraLocalBundleImporter.ImportResult =
        withContext(Dispatchers.IO) {
            require(candidate.canImport) {
                "This catalog entry is not approved for execution. App status, publisher trust and execution policy must all be verified."
            }
            require(candidate.packageName.isNotBlank() && candidate.targetVersions.isNotEmpty()) {
                "Catalog compatibility metadata is incomplete."
            }
            val directory = File(cacheDirectory, "vyra-catalog")
            require(directory.exists() || directory.mkdirs()) { "Could not create VYRA patch cache." }
            val output = File.createTempFile("vyra-catalog-", ".mpp", directory)
            try {
                downloadVerified(candidate.artifactUrl, candidate.sha256, output)
                importer.importVerifiedBundle(output, candidate.sha256)
                    ?: throw IllegalStateException("Morphe did not accept the local patch bundle.")
            } finally {
                output.delete()
            }
        }

    private fun downloadVerified(value: String, expectedSha256: String, output: File) {
        var current = validateArtifactUrl(value)
        var connection: HttpURLConnection? = null
        try {
            repeat(MAX_REDIRECTS + 1) { redirects ->
                require(current.host.lowercase() in ARTIFACT_HOSTS) {
                    "Patch artifact host is not approved: ${current.host}"
                }
                connection = (URL(current.toString()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "VYRA-Studio/1.0")
                    setRequestProperty("Accept", "application/octet-stream")
                }
                val status = connection!!.responseCode
                if (status in 300..399) {
                    val location = connection!!.getHeaderField("Location")
                    connection!!.disconnect()
                    connection = null
                    require(redirects < MAX_REDIRECTS && !location.isNullOrBlank()) {
                        "Patch artifact redirect limit exceeded or destination missing."
                    }
                    current = validateArtifactUrl(current.resolve(location).toString())
                } else {
                    require(status == HttpURLConnection.HTTP_OK) {
                        "Patch artifact download failed with HTTP $status."
                    }
                    val length = connection!!.contentLengthLong
                    require(length in -1L..MAX_BUNDLE_BYTES) { "Patch bundle exceeds the 50 MB limit." }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    connection!!.inputStream.use { input ->
                        FileOutputStream(output).use { stream ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= MAX_BUNDLE_BYTES) { "Patch bundle exceeds the 50 MB limit." }
                                digest.update(buffer, 0, count)
                                stream.write(buffer, 0, count)
                            }
                            stream.fd.sync()
                        }
                    }
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    require(actual.equals(expectedSha256, true)) {
                        "Patch bundle SHA-256 mismatch; downloaded file rejected."
                    }
                    return
                }
            }
            error("Patch artifact redirect limit exceeded.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun getJson(value: String): String {
        val uri = checkedCatalogUrl(value)
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VYRA-Studio-Catalog/1.0")
        }
        try {
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "VYRA catalog request failed or redirected."
            }
            val declared = connection.contentLengthLong
            require(declared < 0 || declared <= MAX_JSON_BYTES) { "VYRA catalog document exceeds 1 MB." }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_JSON_BYTES) { "VYRA catalog document exceeds 1 MB." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return String(bytes, Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun checkedCatalogUrl(value: String): URI {
        val uri = URI(value.trim())
        require(uri.scheme.equals("https", true) &&
            uri.host.equals("raw.githubusercontent.com", true) &&
            uri.userInfo == null && uri.fragment == null &&
            uri.path.startsWith(REPOSITORY_PATH)) {
            "VYRA catalog URL is outside the approved repository."
        }
        return uri
    }

    private fun validateArtifactUrl(value: String): URI {
        val uri = URI(value.trim())
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.fragment == null) {
            "Patch artifacts must use HTTPS without credentials or fragments."
        }
        return uri
    }

    private fun JSONArray?.stringValues(): List<String> {
        if (this == null) return emptyList()
        return buildList { for (i in 0 until length()) add(optString(i)) }
    }

    private fun JSONArray.associateByString(key: String): Map<String, JSONObject> =
        buildMap {
            for (i in 0 until length()) {
                val item = getJSONObject(i)
                val id = item.optString(key)
                require(id.isNotBlank() && put(id, item) == null) {
                    "VYRA catalog contains an empty or duplicate source id."
                }
            }
        }

    private companion object {
        const val ROOT_URL = "https://raw.githubusercontent.com/mm2ultimatehub-rgb/VYRA-REPOSITORY/main/index.json"
        const val REPOSITORY_PATH = "/mm2ultimatehub-rgb/VYRA-REPOSITORY/main/"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_JSON_BYTES = 1_048_576
        const val MAX_BUNDLE_BYTES = 50L * 1024L * 1024L
        const val MAX_REDIRECTS = 5
        const val TRUSTED_OFFICIAL_RELEASE = "2.3.0"
        const val TRUSTED_OFFICIAL_ARTIFACT_URL = "https://github.com/MorpheApp/morphe-patches/releases/download/v2.3.0/patches-2.3.0.mpp"
        const val TRUSTED_OFFICIAL_SHA256 = "1c37549274a2b6cf7f2e3e099d9b8cb641880b8a7dac323a95638a06bb4509a9"
        val SHA256_PATTERN = Regex("[a-f0-9]{64}")
        val RECORD_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}\\.json")
        val ARTIFACT_HOSTS = setOf(
            "github.com",
            "release-assets.githubusercontent.com",
            "objects.githubusercontent.com",
            "github-releases.githubusercontent.com"
        )
    }
}
