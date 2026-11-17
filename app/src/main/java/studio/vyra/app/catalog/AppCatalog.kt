package studio.vyra.app.catalog

enum class CatalogAvailability(val label: String, val detail: String) {
    OFFICIAL("Official patch source", "Listed by the official Morphe patch collection; exact APK version compatibility must still be checked."),
    COMMUNITY("Community sources", "Community bundles are listed; publisher, release integrity, and exact version compatibility must be verified."),
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
        CatalogApp("Discord", "com.discord", CatalogAvailability.COMMUNITY),
        CatalogApp("Instagram", "com.instagram.android", CatalogAvailability.COMMUNITY),
        CatalogApp("TikTok", "com.zhiliaoapp.musically", CatalogAvailability.COMMUNITY),
        CatalogApp("Telegram", "org.telegram.messenger", CatalogAvailability.COMMUNITY),
        CatalogApp("Spotify", "com.spotify.music", CatalogAvailability.COMMUNITY),
        CatalogApp("Facebook", "com.facebook.katana", CatalogAvailability.COMMUNITY),
        CatalogApp("X", "com.twitter.android", CatalogAvailability.COMMUNITY),
        CatalogApp("Threads", "com.instagram.barcelona", CatalogAvailability.COMMUNITY),
        CatalogApp("Twitch", "tv.twitch.android.app", CatalogAvailability.COMMUNITY),
        CatalogApp("Pinterest", "com.pinterest", CatalogAvailability.COMMUNITY),
        CatalogApp("Messenger", "com.facebook.orca", CatalogAvailability.COMMUNITY),
    )
}
