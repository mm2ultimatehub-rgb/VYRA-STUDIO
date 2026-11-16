package studio.vyra.app.repository

import java.net.URI
import org.json.JSONException
import org.json.JSONObject

class RepositoryIndexParser {
    fun parse(json: String): PatchRepository {
        try {
            val root = JSONObject(json)
            require(root.optInt("schemaVersion", -1) == 1) {
                "Unsupported repository schema version."
            }

            val repository = root.getJSONObject("repository")
            val id = repository.requiredString("id")
            require(ID_PATTERN.matches(id)) { "Repository id has an invalid format." }

            val patchesJson = root.getJSONArray("patches")
            val patches = buildList {
                val seenIds = mutableSetOf<String>()
                for (index in 0 until patchesJson.length()) {
                    val item = patchesJson.getJSONObject(index)
                    val patchId = item.requiredString("id")
                    require(ID_PATTERN.matches(patchId)) { "Patch id has an invalid format." }
                    require(seenIds.add(patchId)) { "Duplicate patch id: $patchId" }

                    val target = item.getJSONObject("target")
                    val minVersion = target.optionalLong("minVersionCode")
                    val maxVersion = target.optionalLong("maxVersionCode")
                    require(minVersion == null || minVersion >= 0) { "minVersionCode cannot be negative." }
                    require(maxVersion == null || maxVersion >= 0) { "maxVersionCode cannot be negative." }
                    require(minVersion == null || maxVersion == null || minVersion <= maxVersion) {
                        "minVersionCode cannot exceed maxVersionCode."
                    }

                    val targetPackage = target.requiredString("packageName")
                    require(PACKAGE_PATTERN.matches(targetPackage)) {
                        "Target package name has an invalid format."
                    }

                    val artifact = item.getJSONObject("artifact")
                    val format = artifact.optString("format", "mpp")
                    require(format == "mpp") { "Unsupported patch artifact format: $format" }
                    val artifactUrl = artifact.requiredString("url")
                    require(isHttpsUrl(artifactUrl)) { "Patch artifacts must use HTTPS URLs." }
                    val sha256 = artifact.requiredString("sha256").lowercase()
                    require(SHA256_PATTERN.matches(sha256)) { "Artifact SHA-256 must contain 64 hexadecimal characters." }

                    add(
                        PatchEntry(
                            id = patchId,
                            name = item.requiredString("name"),
                            description = item.optString("description", ""),
                            targetPackage = targetPackage,
                            minVersionCode = minVersion,
                            maxVersionCode = maxVersion,
                            artifactUrl = artifactUrl,
                            artifactSha256 = sha256,
                            format = format,
                        ),
                    )
                }
            }

            return PatchRepository(
                id = id,
                name = repository.requiredString("name"),
                description = repository.optString("description", ""),
                homepage = repository.optString("homepage").takeIf { it.isNotBlank() },
                patches = patches,
            )
        } catch (error: JSONException) {
            throw IllegalArgumentException("Repository index is missing required JSON data.", error)
        }
    }

    private fun JSONObject.requiredString(key: String): String =
        optString(key).trim().also { value ->
            require(value.isNotEmpty() && value != "null") { "Missing required field: $key" }
        }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    private fun isHttpsUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private companion object {
        val ID_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{1,79}")
        val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
        val PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
    }
}
