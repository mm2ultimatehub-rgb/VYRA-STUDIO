package studio.vyra.app.catalog

enum class CatalogAvailability(val label: String, val detail: String) {
    OFFICIAL("Official patch source", "Listed by the official Morphe patch collection; exact APK version compatibility must still be checked."),
    COMMUNITY("Community patch source", "A community bundle is listed; publisher, release integrity, and exact version compatibility must be verified."),
    CATALOG_ONLY("Catalog listing only", "No reviewed VYRA patch bundle is available yet. Open the official app store listing; VYRA patching and in-app updates are not available for this app yet."),
}

data class CatalogApp(
    val name: String,
    val packageName: String,
    val availability: CatalogAvailability,
)

object AppCatalog {
    val apps = listOf(
        CatalogApp("YouTube", "com.google.android.youtube", CatalogAvailability.OFFICIAL),
        CatalogApp("YouTube Music", "com.google.android.apps.youtube.music", CatalogAvailability.OFFICIAL),
        CatalogApp("Reddit", "com.reddit.frontpage", CatalogAvailability.OFFICIAL),
        CatalogApp("Discord", "com.discord", CatalogAvailability.CATALOG_ONLY),
        CatalogApp("Instagram", "com.instagram.android", CatalogAvailability.COMMUNITY),
        CatalogApp("TikTok", "com.zhiliaoapp.musically", CatalogAvailability.COMMUNITY),
        CatalogApp("Telegram", "org.telegram.messenger", CatalogAvailability.CATALOG_ONLY),
        CatalogApp("Spotify", "com.spotify.music", CatalogAvailability.CATALOG_ONLY),
        CatalogApp("Facebook", "com.facebook.katana", CatalogAvailability.CATALOG_ONLY),
        CatalogApp("X", "com.twitter.android", CatalogAvailability.COMMUNITY),
        CatalogApp("Threads", "com.instagram.barcelona", CatalogAvailability.COMMUNITY),
        CatalogApp("Twitch", "tv.twitch.android.app", CatalogAvailability.CATALOG_ONLY),
        CatalogApp("Pinterest", "com.pinterest", CatalogAvailability.CATALOG_ONLY),
        CatalogApp("Messenger", "com.facebook.orca", CatalogAvailability.CATALOG_ONLY),
    )
}
