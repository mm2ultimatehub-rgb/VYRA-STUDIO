package studio.vyra.app.catalog

import org.json.JSONArray
import studio.vyra.app.repository.PatchEntry
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

data class RemoteCatalogApp(
    val id: String,
    val name: String,
    val packageName: String,
    val category: String,
    val status: String,
    val patchSourceIds: List<String>,
    val targetVersions: Map<String, List<String>>,
)

data class RemotePatchSource(
    val id: String,
    val publisher: String,
    val kind: String,
    val releaseVersion: String?,
    val trust: String,
    val execution: String,
    val assetName: String?,
    val assetUrl: String?,
    val sha256: String?,
)

data class RemoteVyraCatalog(
    val apps: List<RemoteCatalogApp>,
    val sources: List<RemotePatchSource>,
    val generatedAt: String,
) {
    fun patchFor(appId: String, sourceId: String): PatchEntry? {
        val app = apps.firstOrNull { it.id == appId } ?: return null
        val source = sources.firstOrNull { it.id == sourceId } ?: return null
        val versions = app.targetVersions[sourceId].orEmpty().toSet()
        val url = source.assetUrl ?: return null
        val digest = source.sha256 ?: return null
        if (versions.isEmpty()) return null
        return PatchEntry(
            id = appId + "-" + sourceId,
            name = app.name + " patch bundle",
            description = "Catalog-provided " + source.publisher + " bundle " + source.releaseVersion.orEmpty(),
            targetPackage = app.packageName,
            minVersionCode = null,
            maxVersionCode = null,
            artifactUrl = url,
            artifactSha256 = digest,
            format = "mpp",
            targetVersions = versions,
            expectedArtifactVersion = source.releaseVersion,
        )
    }
}

class VyraRepositoryCatalogClient {
    fun fetch(): RemoteVyraCatalog {
        val root = JSONObject(getJson(ROOT_URL))
        require(root.optInt("schemaVersion", -1) == 1) { "Unsupported VYRA catalog schema." }
        val catalog = root.getJSONObject("catalog")
        val appsUrl = checkedCatalogUrl(catalog.getString("apps"))
        val sourcesUrl = checkedCatalogUrl(catalog.getString("patchSources"))

        val appIndex = JSONObject(getJson(appsUrl.toString()))
        require(appIndex.optInt("schemaVersion", -1) == 1) { "Unsupported app catalog schema." }
        val sourceIndex = JSONObject(getJson(sourcesUrl.toString()))
        require(sourceIndex.optInt("schemaVersion", -1) == 1) { "Unsupported patch-source schema." }

        val sources = parseSources(sourceIndex.getJSONArray("sources"))
        val sourceIds = sources.map { it.id }.toSet()
        val apps = parseApps(appIndex.getJSONArray("apps"), sourceIds, appsUrl.toString())
        return RemoteVyraCatalog(
            apps = apps,
            sources = sources,
            generatedAt = root.optString("generatedAt", ""),
        )
    }

    private fun parseApps(items: JSONArray, sourceIds: Set<String>, indexUrl: String): List<RemoteCatalogApp> {
        require(items.length() in 1..MAX_APPS) { "App catalog must contain between 1 and $MAX_APPS apps." }
        val seen = mutableSetOf<String>()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.required("id")
                require(ID_PATTERN.matches(id) && seen.add(id)) { "Invalid or duplicate app id: $id" }
                val name = item.required("name")
                val packageName = item.required("packageName")
                require(PACKAGE_PATTERN.matches(packageName)) { "Invalid package name for $id." }
                val status = item.required("status")
                require(status in APP_STATUSES) { "Unknown app status: $status" }
                val sourceArray = item.optJSONArray("patchSourceIds") ?: JSONArray()
                val appSources = sourceArray.toStringList()
                require(appSources.all { it in sourceIds }) { "App $id references an unknown patch source." }

                val recordName = item.required("appRecord")
                require(RECORD_PATTERN.matches(recordName)) { "Invalid app record path." }
                val recordUrl = indexUrl.substringBeforeLast('/') + "/" + recordName
                val record = JSONObject(getJson(checkedCatalogUrl(recordUrl).toString()))
                require(record.optInt("schemaVersion", -1) == 1) { "Unsupported app record schema for $id." }
                require(record.required("id") == id && record.required("packageName") == packageName) {
                    "App record identity mismatch for $id."
                }
                val targets = mutableMapOf<String, List<String>>()
                val patchSources = record.optJSONArray("patchSources") ?: JSONArray()
                for (j in 0 until patchSources.length()) {
                    val patch = patchSources.getJSONObject(j)
                    val sourceId = patch.required("sourceId")
                    require(sourceId in appSources) { "App record references an unlisted patch source." }
                    val versions = (patch.optJSONArray("targetVersions") ?: JSONArray()).toStringList()
                    targets[sourceId] = versions
                }
                add(
                    RemoteCatalogApp(
                        id = id,
                        name = name,
                        packageName = packageName,
                        category = item.optString("category", "other"),
                        status = status,
                        patchSourceIds = appSources,
                        targetVersions = targets,
                    ),
                )
            }
        }
    }

    private fun parseSources(items: JSONArray): List<RemotePatchSource> {
        require(items.length() in 1..MAX_SOURCES) { "Patch-source catalog is empty or too large." }
        val seen = mutableSetOf<String>()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.required("id")
                require(ID_PATTERN.matches(id) && seen.add(id)) { "Invalid or duplicate patch-source id: $id" }
                val release = item.optJSONObject("latestRelease")
                add(
                    RemotePatchSource(
                        id = id,
                        publisher = item.required("publisher"),
                        kind = item.optString("kind", "unknown"),
                        releaseVersion = release?.optString("version")?.takeIf { !it.isNullOrBlank() },
                        trust = item.optString("trust", "unverified"),
                        execution = item.optString("execution", "disabled"),
                        assetName = release?.optString("assetName")?.takeIf { !it.isNullOrBlank() },
                        assetUrl = release?.optString("assetUrl")?.takeIf { !it.isNullOrBlank() }?.also { validateArtifactUrl(it) },
                        sha256 = release?.optString("sha256")?.takeIf { !it.isNullOrBlank() }?.lowercase()?.also {
                            require(SHA256_PATTERN.matches(it)) { "Patch source $id has an invalid SHA-256." }
                        },
                    ),
                )
            }
        }
    }

    private fun validateArtifactUrl(value: String) {
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { throw IllegalArgumentException("Patch artifact URL is invalid.", it) }
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.fragment == null) {
            "Patch artifacts must use valid HTTPS URLs."
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
            val status = connection.responseCode
            require(status == HttpURLConnection.HTTP_OK) {
                if (status in 300..399) "Catalog redirects are not accepted."
                else "Catalog request failed with HTTP $status."
            }
            val declared = connection.contentLengthLong
            require(declared < 0 || declared <= MAX_JSON_BYTES) { "Catalog document exceeds the 1 MB limit." }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_JSON_BYTES) { "Catalog document exceeds the 1 MB limit." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return String(bytes, StandardCharsets.UTF_8)
        } catch (error: JSONException) {
            throw IllegalArgumentException("Catalog contains invalid JSON.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun checkedCatalogUrl(value: String): URI {
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { throw IllegalArgumentException("Catalog contains an invalid URL.", it) }
        require(uri.scheme.equals("https", true) && uri.host.equals("raw.githubusercontent.com", true)) {
            "VYRA catalog files must be hosted on the approved HTTPS GitHub raw host."
        }
        require(uri.userInfo == null && uri.fragment == null && uri.path.startsWith(REPOSITORY_PATH)) {
            "Catalog URL points outside the configured VYRA repository."
        }
        return uri
    }

    private fun JSONObject.required(key: String): String =
        optString(key).trim().also { require(it.isNotEmpty() && it != "null") { "Missing catalog field: $key" } }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (i in 0 until length()) {
            val value = optString(i).trim()
            require(value.isNotEmpty() && value != "null") { "Catalog list contains an empty value." }
            add(value)
        }
    }

    private companion object {
        const val ROOT_URL = "https://raw.githubusercontent.com/mm2ultimatehub-rgb/VYRA-REPOSITORY/main/index.json"
        const val REPOSITORY_PATH = "/mm2ultimatehub-rgb/VYRA-REPOSITORY/main/"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_JSON_BYTES = 1_048_576
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
        const val MAX_APPS = 100
        const val MAX_SOURCES = 50
        val ID_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{1,79}")
        val RECORD_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}\\.json")
        val PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
        val APP_STATUSES = setOf("candidate", "experimental", "verified", "unsupported")
    }
}
