/*
 * VYRA integration overlay for the pinned Morphe Manager runtime.
 *
 * This file is distributed as part of VYRA-STUDIO and is copied into the
 * pinned Morphe Manager source tree by scripts/apply-vyra-morphe-overlay.sh.
 * Morphe Manager is GPL-3.0; see upstream notices and VYRA attribution docs.
 */
package app.morphe.manager.domain.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile

/**
 * Imports a catalog-downloaded MPP into Morphe's local bundle repository.
 *
 * This verifies that the downloaded bytes still match the catalog digest before
 * handing the bundle to Morphe. Importing makes patch metadata available to the
 * engine; it does not itself select or execute any patch.
 */
class VyraLocalBundleImporter(
    private val patchBundleRepository: PatchBundleRepository
) {
    data class ImportResult(
        val sourceUid: Int,
        val sha256: String,
        val sizeBytes: Long
    )

    suspend fun importVerifiedBundle(
        bundleFile: File,
        expectedSha256: String
    ): ImportResult? {
        require(expectedSha256.matches(Regex("(?i)^[a-f0-9]{64}$"))) {
            "A valid SHA-256 digest is required."
        }

        val file = bundleFile.canonicalFile
        require(file.isFile) { "The patch bundle does not exist." }
        require(file.length() in 1..MAX_BUNDLE_BYTES) {
            "The patch bundle is empty or exceeds the 50 MB limit."
        }

        inspectMppArchive(file)

        val actualSha256 = withContext(Dispatchers.IO) {
            file.inputStream().buffered().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }

        require(actualSha256.equals(expectedSha256, ignoreCase = true)) {
            "Patch bundle SHA-256 does not match the catalog."
        }

        val uid = patchBundleRepository.createLocal(file.length()) {
            file.inputStream()
        } ?: return null

        return ImportResult(
            sourceUid = uid,
            sha256 = actualSha256,
            sizeBytes = file.length()
        )
    }

    private fun inspectMppArchive(file: File) {
        JarFile(file, false).use { jar ->
            require(jar.manifest != null) { "The patch bundle has no JAR manifest." }

            var entries = 0
            var dexEntries = 0
            var expandedBytes = 0L
            val enumeration = jar.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement()
                entries++
                require(entries <= MAX_ARCHIVE_ENTRIES) {
                    "The patch bundle contains too many archive entries."
                }
                require(!entry.name.startsWith("/") && !entry.name.split('/').contains("..")) {
                    "The patch bundle contains an unsafe archive path."
                }
                if (entry.name.endsWith(".dex", ignoreCase = true)) dexEntries++
                if (entry.size > 0L) {
                    require(entry.size <= MAX_EXPANDED_BYTES - expandedBytes) {
                        "The patch bundle expands beyond the 250 MB safety limit."
                    }
                    expandedBytes += entry.size
                }
            }

            require(dexEntries > 0) { "The patch bundle contains no DEX payload." }
        }
    }

    private companion object {
        const val MAX_BUNDLE_BYTES = 50L * 1024L * 1024L
        const val MAX_ARCHIVE_ENTRIES = 50_000
        const val MAX_EXPANDED_BYTES = 250L * 1024L * 1024L
    }
}
