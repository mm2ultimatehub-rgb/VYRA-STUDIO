package app.morphe.manager.domain.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VyraCatalogRepositoryTest {
    private fun candidate(
        appStatus: String = "candidate",
        trust: String = "official-publisher",
        execution: String = "disabled-until-reviewed",
        versions: Set<String> = setOf("21.30.216")
    ) = VyraCatalogRepository.Candidate(
        appId = "youtube",
        appName = "YouTube",
        packageName = "com.google.android.youtube",
        appStatus = appStatus,
        sourceId = "morphe-official",
        publisher = "MorpheApp",
        trust = trust,
        execution = execution,
        releaseVersion = "2.3.0",
        artifactUrl = "https://github.com/MorpheApp/morphe-patches/releases/download/v2.3.0/patches-2.3.0.mpp",
        sha256 = "1".repeat(64),
        targetVersions = versions
    )

    @Test
    fun `candidate catalog entries are not executable`() {
        assertFalse(candidate().canImport)
    }

    @Test
    fun `third party publishers remain blocked even when marked verified`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                trust = "experimental-third-party",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `official verified and explicitly enabled source may be imported`() {
        assertTrue(
            candidate(
                appStatus = "verified",
                trust = "official-publisher",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `empty compatibility metadata blocks import`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                trust = "official-publisher",
                execution = "enabled",
                versions = emptySet()
            ).canImport
        )
    }
}
