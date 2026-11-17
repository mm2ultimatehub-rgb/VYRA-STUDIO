package app.morphe.manager.domain.repository

import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MppArchiveValidatorTest {
    @Test
    fun `accepts a manifest and dex payload within limits`() {
        val file = archive("classes.dex" to byteArrayOf(1, 2, 3))
        try {
            MppArchiveValidator.inspect(file)
            assertTrue(file.length() > 0)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects archives without dex payload`() {
        val file = archive("assets/readme.txt" to byteArrayOf(1))
        try {
            assertFailsWith<IllegalArgumentException> {
                MppArchiveValidator.inspect(file)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects traversal paths`() {
        val file = archive("../escape.dex" to byteArrayOf(1))
        try {
            assertFailsWith<IllegalArgumentException> {
                MppArchiveValidator.inspect(file)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects windows style path separators`() {
        val file = archive("folder\\\\..\\\\escape.dex" to byteArrayOf(1))
        try {
            assertFailsWith<IllegalArgumentException> {
                MppArchiveValidator.inspect(file)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `counts actual decompressed bytes against the configured limit`() {
        val file = archive("classes.dex" to ByteArray(64) { 7 })
        try {
            assertFailsWith<IllegalArgumentException> {
                MppArchiveValidator.inspect(file, maxExpandedBytes = 16)
            }
        } finally {
            file.delete()
        }
    }


    @Test
    fun `validates the pinned official Morphe release when CI provides it`() {
        val fixturePath = System.getProperty("vyra.test.mpp") ?: return
        val fixture = File(fixturePath)
        assertTrue(fixture.isFile, "CI-provided official MPP fixture is missing.")
        MppArchiveValidator.inspect(fixture)
    }

    private fun archive(vararg entries: Pair<String, ByteArray>): File {
        val file = File.createTempFile("vyra-mpp-test-", ".mpp")
        val manifest = Manifest().apply {
            mainAttributes.putValue("Manifest-Version", "1.0")
        }
        JarOutputStream(file.outputStream(), manifest).use { output ->
            entries.forEach { (name, bytes) ->
                output.putNextEntry(JarEntry(name))
                output.write(bytes)
                output.closeEntry()
            }
        }
        return file
    }
}
