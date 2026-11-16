package studio.vyra.app.repository

data class PatchRepository(
    val id: String,
    val name: String,
    val description: String,
    val homepage: String?,
    val patches: List<PatchEntry>,
)

data class PatchEntry(
    val id: String,
    val name: String,
    val description: String,
    val targetPackage: String,
    val minVersionCode: Long?,
    val maxVersionCode: Long?,
    val artifactUrl: String,
    val artifactSha256: String,
    val format: String,
)
