/*
 * Defensive MPP/JAR archive validation for the VYRA import bridge.
 * This validates archive structure only; it does not establish publisher trust.
 */
package app.morphe.manager.domain.repository

import java.io.File
import java.util.jar.JarFile

internal object MppArchiveValidator {
    private const val MAX_ARCHIVE_ENTRIES = 50_000
    private const val DEFAULT_MAX_EXPANDED_BYTES = 250L * 1024L * 1024L

    fun inspect(
        file: File,
        maxExpandedBytes: Long = DEFAULT_MAX_EXPANDED_BYTES
    ) {
        require(maxExpandedBytes > 0L) { "Expanded-size limit must be positive." }
        JarFile(file, false).use { jar ->
            require(jar.manifest != null) { "The patch bundle has no JAR manifest." }

            var entries = 0
            var dexEntries = 0
            var expandedBytes = 0L
            val names = HashSet<String>()
            val enumeration = jar.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement()
                entries++
                require(entries <= MAX_ARCHIVE_ENTRIES) {
                    "The patch bundle contains too many archive entries."
                }

                val name = entry.name
                val path = name.removeSuffix("/")
                val segments = path.split('/')
                require(
                    name.isNotBlank() &&
                        !name.startsWith("/") &&
                        !name.contains('\\\\') &&
                        !name.contains('\u0000') &&
                        !Regex("^[A-Za-z]:").containsMatchIn(name) &&
                        segments.none { it.isEmpty() || it == "." || it == ".." } &&
                        names.add(name)
                ) {
                    "The patch bundle contains an unsafe or duplicate archive path."
                }

                if (name.endsWith(".dex", ignoreCase = true)) dexEntries++

                // Enforce the real decompressed byte count. ZIP central-directory
                // sizes alone are not sufficient for malformed or adversarial entries.
                jar.getInputStream(entry).use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(count.toLong() <= maxExpandedBytes - expandedBytes) {
                            "The patch bundle expands beyond the safety limit."
                        }
                        expandedBytes += count
                    }
                }
            }

            require(dexEntries > 0) { "The patch bundle contains no DEX payload." }
        }
    }
}
