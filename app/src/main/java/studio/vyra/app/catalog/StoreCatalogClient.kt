package studio.vyra.app.catalog

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONException
import org.json.JSONObject

data class StoreCatalogEntry(
    val id: String,
    val name: String,
    val packageName: String,
    val category: String,
    val status: String,
    val patchSourceIds: List<String>,
    val iconUrl: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val downloadUrl: String? = null,
    val sha256: String? = null,
)

class StoreCatalogClient(
    private val indexUrl: String = DEFAULT_INDEX_URL,
) {
    fun fetch(): List<StoreCatalogEntry> {
        val uri = validateIndexUrl(indexUrl)
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VYRA-Store/0.1")
        }

        try {
            val statusCode = connection.responseCode
            require(statusCode == HttpURLConnection.HTTP_OK) {
                if (statusCode in 300..399) {
                    "The VYRA catalog redirected. Use its final HTTPS URL."
                } else {
                    "VYRA catalog request failed with HTTP $statusCode."
                }
            }

            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= MAX_INDEX_BYTES) {
                "VYRA catalog is larger than the 1 MB limit."
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
                        "VYRA catalog is larger than the 1 MB limit."
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return parse(String(bytes, StandardCharsets.UTF_8))
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: String): List<StoreCatalogEntry> {
        try {
            val root = JSONObject(json)
            require(root.optInt("schemaVersion", -1) == 1) {
                "Unsupported VYRA app catalog version."
            }
            val apps = root.getJSONArray("apps")
            require(apps.length() <= MAX_APPS) {
                "VYRA catalog contains too many app entries."
            }

            val seenIds = mutableSetOf<String>()
            val seenPackages = mutableSetOf<String>()
            return buildList {
                for (index in 0 until apps.length()) {
                    val item = apps.getJSONObject(index)
                    val id = item.requiredString("id")
                    val name = item.requiredString("name")
                    val packageName = item.requiredString("packageName")
                    val category = item.requiredString("category")
                    val availability = item.requiredString("status")
                    require(ID_PATTERN.matches(id)) { "Invalid app id: $id" }
                    require(PACKAGE_PATTERN.matches(packageName)) {
                        "Invalid Android package name for $name."
                    }
                    require(availability in ALLOWED_STATUSES) {
                        "Unsupported availability status for $name."
                    }
                    require(seenIds.add(id)) { "Duplicate app id: $id" }
                    require(seenPackages.add(packageName)) {
                        "Duplicate Android package: $packageName"
                    }

                    val iconUrl = item.optString("iconUrl").trim().takeIf { it.isNotEmpty() && it != "null" }
                    val downloadUrl = item.optString("downloadUrl").trim().takeIf { it.isNotEmpty() && it != "null" }
                    val versionName = item.optString("versionName").trim().takeIf { it.isNotEmpty() && it != "null" }
                    val versionCode = if (item.has("versionCode") && !item.isNull("versionCode")) item.getLong("versionCode") else null
                    val sha256 = item.optString("sha256").trim().takeIf { it.isNotEmpty() && it != "null" }?.lowercase()
                    require(iconUrl == null || isHttpsUrl(iconUrl)) { "App icons must use HTTPS URLs." }
                    require(downloadUrl == null || isHttpsUrl(downloadUrl)) { "App downloads must use HTTPS URLs." }
                    require(sha256 == null || SHA256_PATTERN.matches(sha256)) { "App APK SHA-256 must contain 64 hexadecimal characters." }
                    require(versionCode == null || versionCode >= 0) { "App versionCode cannot be negative." }

                    val sources = item.optJSONArray("patchSourceIds")
                    val sourceIds = buildList {
                        if (sources != null) {
                            for (sourceIndex in 0 until sources.length()) {
                                val sourceId = sources.optString(sourceIndex).trim()
                                require(sourceId.isNotEmpty()) {
                                    "Empty patch source id for $name."
                                }
                                add(sourceId)
                            }
                        }
                    }
                    add(
                        StoreCatalogEntry(
                            id = id,
                            name = name,
                            packageName = packageName,
                            category = category,
                            status = availability,
                            patchSourceIds = sourceIds,
                            iconUrl = iconUrl,
                            versionName = versionName,
                            versionCode = versionCode,
                            downloadUrl = downloadUrl,
                            sha256 = sha256,
                        ),
                    )
                }
            }
        } catch (error: JSONException) {
            throw IllegalArgumentException("VYRA catalog is missing required JSON data.", error)
        }
    }

    private fun JSONObject.requiredString(key: String): String =
        optString(key).trim().also { value ->
            require(value.isNotEmpty() && value != "null") {
                "VYRA catalog is missing required field: $key"
            }
        }

    private fun isHttpsUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun validateIndexUrl(value: String): URI {
        require(value.length <= MAX_URL_LENGTH) { "VYRA catalog URL is too long." }
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { throw IllegalArgumentException("VYRA catalog URL is invalid.", it) }
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "VYRA catalog must use HTTPS."
        }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) {
            "VYRA catalog URL cannot contain credentials or a fragment."
        }
        return uri
    }

    private companion object {
        const val DEFAULT_INDEX_URL =
            "https://raw.githubusercontent.com/mm2ultimatehub-rgb/VYRA-REPOSITORY/main/apps/index.json"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_INDEX_BYTES = 1_048_576
        const val MAX_URL_LENGTH = 2_048
        const val MAX_APPS = 100
        val ID_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{1,79}")
        val PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
        val ALLOWED_STATUSES = setOf(
            "candidate",
            "experimental",
            "verified",
            "unsupported",
            "catalog-only",
        )
    }
}
