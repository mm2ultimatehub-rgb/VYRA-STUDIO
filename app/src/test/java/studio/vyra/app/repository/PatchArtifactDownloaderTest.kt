package studio.vyra.app.repository

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PatchArtifactDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun reusesOnlyCachedArtifactWithExpectedDigest() {
        val bytes = "verified test bundle".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val cache = temporaryFolder.newFolder("cache")
        val verifiedDirectory = File(cache, "verified-patches").apply { mkdirs() }
        val patch = patch(digest)
        val cached = File(verifiedDirectory, "${patch.id}-${digest.take(12)}.mpp")
        cached.writeBytes(bytes)

        val result = PatchArtifactDownloader().downloadAndVerify(patch, cache)

        assertEquals(cached.canonicalFile, result.canonicalFile)
        assertTrue(result.readBytes().contentEquals(bytes))
    }

    @Test
    fun rejectsNonMppArtifactsBeforeNetworkAccess() {
        val patch = patch("a".repeat(64)).copy(format = "zip")

        val error = runCatching {
            PatchArtifactDownloader().downloadAndVerify(patch, temporaryFolder.root)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Unsupported patch artifact format.", error?.message)
    }

    @Test
    fun rejectsUnapprovedInitialArtifactHostBeforeNetworkAccess() {
        val patch = patch("a".repeat(64)).copy(
            artifactUrl = "https://attacker.example/patches.mpp",
        )

        val error = runCatching {
            PatchArtifactDownloader().downloadAndVerify(patch, temporaryFolder.root)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Patch artifact host is not approved: attacker.example", error?.message)
    }

    @Test
    fun rejectsInvalidSha256BeforeNetworkAccess() {
        val patch = patch("not-a-digest")

        val error = runCatching {
            PatchArtifactDownloader().downloadAndVerify(patch, temporaryFolder.root)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Patch SHA-256 is invalid.", error?.message)
    }

    private fun patch(digest: String) = PatchEntry(
        id = "test-source-patch",
        name = "Test patch",
        description = "",
        targetPackage = "org.example.app",
        minVersionCode = null,
        maxVersionCode = null,
        artifactUrl = "https://github.com/example/test/releases/download/v1/test.mpp",
        artifactSha256 = digest,
        format = "mpp",
    )
}
