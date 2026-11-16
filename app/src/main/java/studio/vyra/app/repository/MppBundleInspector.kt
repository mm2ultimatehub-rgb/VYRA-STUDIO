package studio.vyra.app.repository

import java.io.File
import java.util.jar.JarFile

data class MppBundleMetadata(
    val name: String?,
    val version: String?,
    val description: String?,
    val source: String?,
    val author: String?,
    val contact: String?,
    val website: String?,
    val license: String?,
    val patcherVersion: String?,
    val dexEntryCount: Int,
)

class MppBundleInspector {
    fun inspect(file: File): MppBundleMetadata {
        require(file.isFile) { "Patch bundle file does not exist." }
        require(file.length() in 1..MAX_BUNDLE_BYTES) { "Patch bundle size is invalid or exceeds the 50 MB limit." }

        return try {
            JarFile(file, false).use { jar ->
                val manifest = requireNotNull(jar.manifest) {
                    "Patch bundle is missing its JAR manifest."
                }
                val entries = jar.entries()
                var dexCount = 0
                var entryCount = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    entryCount++
                    require(entryCount <= MAX_ENTRIES) { "Patch bundle contains too many entries." }
                    if (entry.name.endsWith(".dex", ignoreCase = true)) {
                        require(!entry.isDirectory && entry.size > 0L) {
                            "Patch bundle contains an empty DEX entry."
                        }
                        dexCount++
                    }
                }
                require(dexCount > 0) { "Patch bundle does not contain DEX entries." }

                val attributes = manifest.mainAttributes
                fun value(key: String) = attributes.getValue(key)?.trim()?.takeIf { it.isNotEmpty() }

                MppBundleMetadata(
                    name = value("Name"),
                    version = value("Version"),
                    description = value("Description"),
                    source = value("Source"),
                    author = value("Author"),
                    contact = value("Contact"),
                    website = value("Website"),
                    license = value("License"),
                    patcherVersion = value("Patcher-Version"),
                    dexEntryCount = dexCount,
                )
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("Patch bundle is not a readable JAR/MPP archive.", error)
        }
    }

    private companion object {
        const val MAX_BUNDLE_BYTES = 50L * 1024 * 1024
        const val MAX_ENTRIES = 50_000
    }
}
