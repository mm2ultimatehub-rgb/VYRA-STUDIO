package studio.vyra.app.catalog

import studio.vyra.app.apk.ApkInspection
import studio.vyra.app.repository.PatchCompatibility
import studio.vyra.app.repository.PatchEntry

/** Resolves the central catalog into exact-version candidates; it never executes patch code. */
class VyraCatalogPatchProvider(
    private val client: VyraRepositoryCatalogClient = VyraRepositoryCatalogClient(),
) {
    fun fetchCompatible(apk: ApkInspection): List<Pair<String, PatchEntry>> {
        val catalog = client.fetch()
        return buildList {
            catalog.apps.filter { it.packageName == apk.packageName }.forEach { app ->
                app.patchSourceIds.forEach sourceLoop@{ sourceId ->
                    val source = catalog.sources.firstOrNull { it.id == sourceId } ?: return@sourceLoop
                    val patch = catalog.patchFor(app.id, sourceId) ?: return@sourceLoop
                    if (PatchCompatibility.matches(patch, apk)) {
                        val state = "trust=" + source.trust + ", execution=" + source.execution + ", app=" + app.status
                        add(("VYRA Catalog · " + source.publisher + " · " + state) to patch)
                    }
                }
            }
        }
    }
}
