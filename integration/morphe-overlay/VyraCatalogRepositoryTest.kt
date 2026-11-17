package app.morphe.manager.domain.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VyraCatalogRepositoryTest {
    private fun candidate(
        appStatus: String = "candidate",
        patchStatus: String = "verified",
        validationStatus: String = "passed",
        sourceListedForApp: Boolean = true,
        sourceId: String = "morphe-official",
        publisher: String = "MorpheApp",
        repository: String = "https://github.com/MorpheApp/morphe-patches",
        artifactUrl: String = "https://github.com/MorpheApp/morphe-patches/releases/download/v2.3.0/patches-2.3.0.mpp",
        trust: String = "official-publisher",
        execution: String = "disabled-until-reviewed",
        versions: Set<String> = setOf("21.30.216")
    ) = VyraCatalogRepository.Candidate(
        appId = "youtube",
        appName = "YouTube",
        packageName = "com.google.android.youtube",
        appStatus = appStatus,
        patchStatus = patchStatus,
        validationStatus = validationStatus,
        sourceListedForApp = sourceListedForApp,
        sourceId = sourceId,
        publisher = publisher,
        repository = repository,
        trust = trust,
        execution = execution,
        releaseVersion = "2.3.0",
        artifactUrl = artifactUrl,
        sha256 = "1c37549274a2b6cf7f2e3e099d9b8cb641880b8a7dac323a95638a06bb4509a9",
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
    fun `candidate patch record remains blocked even when app is verified`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                patchStatus = "candidate",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `catalog validation must pass before import`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                validationStatus = "not-tested",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `source must be declared by the app record`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                sourceListedForApp = false,
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `only the pinned official Morphe repository is eligible`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                repository = "https://github.com/attacker/morphe-patches",
                execution = "enabled"
            ).canImport
        )
        assertFalse(
            candidate(
                appStatus = "verified",
                sourceId = "community",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `artifact must come from the official release path`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                artifactUrl = "https://github.com/attacker/release/download/file.mpp",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `artifact must match the exact pinned release asset URL`() {
        assertFalse(
            candidate(
                appStatus = "verified",
                artifactUrl = "https://github.com/MorpheApp/morphe-patches/releases/download/v2.3.0/other.mpp",
                execution = "enabled"
            ).canImport
        )
    }

    @Test
    fun `catalog cannot replace the pinned official artifact digest`() {
        val altered = candidate(
            appStatus = "verified",
            execution = "enabled"
        ).copy(sha256 = "2".repeat(64))
        assertFalse(altered.canImport)
    }

    @Test
    fun `catalog cannot select an unreviewed official release`() {
        val altered = candidate(
            appStatus = "verified",
            execution = "enabled"
        ).copy(releaseVersion = "2.4.0")
        assertFalse(altered.canImport)
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
