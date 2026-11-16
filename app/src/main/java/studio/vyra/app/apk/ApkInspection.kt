package studio.vyra.app.apk

data class ApkInspection(
    val fileName: String,
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long,
    val minSdk: Int?,
    val targetSdk: Int?,
    val apkSha256: String,
    val signerSha256: List<String>,
)
