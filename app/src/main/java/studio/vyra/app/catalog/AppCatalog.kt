package studio.vyra.app.catalog

enum class CatalogAvailability(val label: String, val detail: String) {
    CANDIDATE(
        "Candidate · not yet verified",
        "A patch source is listed, but end-to-end patching and installation have not been validated. Automatic patch execution is disabled."
    ),
    EXPERIMENTAL(
        "Experimental · use caution",
        "Only experimental community metadata is listed. Compatibility and safety are not verified; automatic execution is disabled."
    ),
    NO_SOURCE(
        "No compatible source listed",
        "VYRA has no reviewed patch source for this app yet. You can still inspect a local APK."
    ),
}

data class CatalogApp(
    val name: String,
    val packageName: String,
    val availability: CatalogAvailability,
)

object AppCatalog {
    // Keep this list honest: entries reflect the current VYRA repository catalog status.
    val apps = listOf(
        CatalogApp("YouTube", "com.google.android.youtube", CatalogAvailability.CANDIDATE),
        CatalogApp("YouTube Music", "com.google.android.apps.youtube.music", CatalogAvailability.CANDIDATE),
        CatalogApp("Reddit", "com.reddit.frontpage", CatalogAvailability.CANDIDATE),
        CatalogApp("Instagram", "com.instagram.android", CatalogAvailability.EXPERIMENTAL),
        CatalogApp("TikTok", "com.zhiliaoapp.musically", CatalogAvailability.EXPERIMENTAL),
        CatalogApp("Threads", "com.instagram.barcelona", CatalogAvailability.EXPERIMENTAL),
        CatalogApp("X", "com.twitter.android", CatalogAvailability.EXPERIMENTAL),
        CatalogApp("Discord", "com.discord", CatalogAvailability.NO_SOURCE),
        CatalogApp("Telegram", "org.telegram.messenger", CatalogAvailability.NO_SOURCE),
        CatalogApp("Spotify", "com.spotify.music", CatalogAvailability.NO_SOURCE),
        CatalogApp("Facebook", "com.facebook.katana", CatalogAvailability.NO_SOURCE),
        CatalogApp("Twitch", "tv.twitch.android.app", CatalogAvailability.NO_SOURCE),
        CatalogApp("Pinterest", "com.pinterest", CatalogAvailability.NO_SOURCE),
        CatalogApp("Messenger", "com.facebook.orca", CatalogAvailability.NO_SOURCE),
    )
}
