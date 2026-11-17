package studio.vyra.app.repository

import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MppBundleInspectorTest {
    private val inspector = MppBundleInspector()

    @Test
    fun readsManifestAndDexCountWithoutExecutingPatchCode() {
        val file = createBundle(includeDex = true)
        try {
            val metadata = inspector.inspect(file)
            assertEquals("Test patches", metadata.name)
            assertEquals("1.2.3", metadata.version)
            assertEquals("2.4.0", metadata.patcherVersion)
            assertEquals(2, metadata.dexEntryCount)
        } finally {
            file.delete()
        }
    }

    @Test
    fun acceptsExpectedBundleVersionWithOptionalVPrefix() {
        val file = createBundle(includeDex = true)
        try {
            val metadata = inspector.inspect(file, "v1.2.3")
            assertEquals("1.2.3", metadata.version)
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsBundleVersionThatDoesNotMatchCatalogRelease() {
        val file = createBundle(includeDex = true)
        try {
            val error = assertThrows(IllegalArgumentException::class.java) {
                inspector.inspect(file, "2.3.0")
            }
            assertEquals(
                "Patch bundle version mismatch: expected 2.3.0 but found 1.2.3.",
                error.message,
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsBundleWithoutManifest() {
        val file = createBundle(includeDex = true, includeManifest = false)
        try {
            assertThrows(IllegalArgumentException::class.java) { inspector.inspect(file) }
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsBundleWithoutDexEntries() {
        val file = createBundle(includeDex = false)
        try {
            assertThrows(IllegalArgumentException::class.java) { inspector.inspect(file) }
        } finally {
            file.delete()
        }
    }

    private fun createBundle(
        includeDex: Boolean,
        includeManifest: Boolean = true,
    ): File {
        val file = File.createTempFile("vyra-test-", ".mpp")
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes.putValue("Name", "Test patches")
            mainAttributes.putValue("Version", "1.2.3")
            mainAttributes.putValue("Patcher-Version", "2.4.0")
        }
        JarOutputStream(file.outputStream(), manifest).use { jar ->
            if (includeDex) {
                jar.putNextEntry(JarEntry("classes.dex"))
                jar.write(byteArrayOf(1, 2, 3))
                jar.closeEntry()
                jar.putNextEntry(JarEntry("classes2.dex"))
                jar.write(byteArrayOf(4, 5, 6))
                jar.closeEntry()
            }
        }
        if (!includeManifest) {
            // Recreate as a plain ZIP/JAR without a manifest.
            val plain = File.createTempFile("vyra-test-no-manifest-", ".mpp")
            java.util.zip.ZipOutputStream(plain.outputStream()).use { zip ->
                if (includeDex) {
                    zip.putNextEntry(JarEntry("classes.dex"))
                    zip.write(byteArrayOf(1, 2, 3))
                    zip.closeEntry()
                }
            }
            file.delete()
            return plain
        }
        return file
    }
}
