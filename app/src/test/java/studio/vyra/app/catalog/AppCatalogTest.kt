package studio.vyra.app.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCatalogTest {
    @Test
    fun everyCatalogEntryHasAUniquePackageName() {
        val packageNames = AppCatalog.apps.map { it.packageName }
        assertEquals(packageNames.size, packageNames.toSet().size)
        assertTrue(packageNames.all { it.isNotBlank() })
    }

    @Test
    fun catalogContainsTheExpectedCoreApps() {
        val byPackage = AppCatalog.apps.associateBy { it.packageName }
        assertNotNull(byPackage["com.google.android.youtube"])
        assertNotNull(byPackage["com.google.android.apps.youtube.music"])
        assertNotNull(byPackage["com.reddit.frontpage"])
        assertNotNull(byPackage["com.discord"])
        assertNotNull(byPackage["com.spotify.music"])
    }

    @Test
    fun readinessLabelsDoNotOverstatePatchSupport() {
        val byPackage = AppCatalog.apps.associateBy { it.packageName }
        assertEquals(CatalogAvailability.CANDIDATE, byPackage["com.google.android.youtube"]?.availability)
        assertEquals(CatalogAvailability.EXPERIMENTAL, byPackage["com.instagram.android"]?.availability)
        assertEquals(CatalogAvailability.NO_SOURCE, byPackage["com.discord"]?.availability)
        assertEquals(CatalogAvailability.NO_SOURCE, byPackage["com.spotify.music"]?.availability)
    }
}
