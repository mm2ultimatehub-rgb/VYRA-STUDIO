package studio.vyra.app.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.vyra.app.apk.ApkInspection

class PatchCompatibilityTest {
    private val apk = ApkInspection(
        fileName = "sample.apk",
        packageName = "org.example.app",
        appLabel = "Example",
        versionName = "2.0",
        versionCode = 150,
        minSdk = 26,
        targetSdk = 35,
        apkSha256 = "a".repeat(64),
        signerSha256 = emptyList(),
    )

    @Test
    fun matchesPackageAndVersionRange() {
        val patch = patch(min = 120, max = 180)
        assertTrue(PatchCompatibility.matches(patch, apk))
    }

    @Test
    fun rejectsDifferentPackage() {
        val patch = patch(packageName = "org.other.app")
        assertFalse(PatchCompatibility.matches(patch, apk))
    }

    @Test
    fun matchesExactVersionNameAllowlist() {
        val allowed = patch().copy(targetVersions = setOf("2.0", "2.1"))
        val rejected = patch().copy(targetVersions = setOf("2.1", "2.2"))
        assertTrue(PatchCompatibility.matches(allowed, apk))
        assertFalse(PatchCompatibility.matches(rejected, apk))
    }

    @Test
    fun rejectsMissingVersionNameWhenAllowlistExists() {
        val withoutVersion = apk.copy(versionName = null)
        val restricted = patch().copy(targetVersions = setOf("2.0"))
        assertFalse(PatchCompatibility.matches(restricted, withoutVersion))
    }

    @Test
    fun rejectsVersionOutsideRange() {
        assertFalse(PatchCompatibility.matches(patch(min = 151), apk))
        assertFalse(PatchCompatibility.matches(patch(max = 149), apk))
    }

    private fun patch(
        packageName: String = "org.example.app",
        min: Long? = null,
        max: Long? = null,
    ) = PatchEntry(
        id = "sample-patch",
        name = "Sample",
        description = "",
        targetPackage = packageName,
        minVersionCode = min,
        maxVersionCode = max,
        artifactUrl = "https://example.org/sample.mpp",
        artifactSha256 = "b".repeat(64),
        format = "mpp",
    )
}
