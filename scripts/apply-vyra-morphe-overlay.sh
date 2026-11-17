#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANAGER="$ROOT/upstream/morphe-manager"
EXPECTED_MANAGER_SHA="d9f56746d13890c0f8837c6d070dae6f210ad2a9"
SOURCE="$ROOT/integration/morphe-overlay/VyraLocalBundleImporter.kt"
CATALOG_SOURCE="$ROOT/integration/morphe-overlay/VyraCatalogRepository.kt"
TEST_SOURCE="$ROOT/integration/morphe-overlay/VyraCatalogRepositoryTest.kt"
VALIDATOR_SOURCE="$ROOT/integration/morphe-overlay/MppArchiveValidator.kt"
VALIDATOR_TEST_SOURCE="$ROOT/integration/morphe-overlay/MppArchiveValidatorTest.kt"
STORE_SOURCE="$ROOT/integration/morphe-overlay/VyraStorePage.kt"
TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/VyraLocalBundleImporter.kt"
CATALOG_TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/VyraCatalogRepository.kt"
TEST_TARGET="$MANAGER/app/src/test/java/app/morphe/manager/domain/repository/VyraCatalogRepositoryTest.kt"
VALIDATOR_TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/MppArchiveValidator.kt"
VALIDATOR_TEST_TARGET="$MANAGER/app/src/test/java/app/morphe/manager/domain/repository/MppArchiveValidatorTest.kt"
STORE_TARGET="$MANAGER/app/src/main/java/app/morphe/manager/ui/screen/vyra/VyraStorePage.kt"
MODULE="$MANAGER/app/src/main/java/app/morphe/manager/di/RepositoryModule.kt"

if ! git -C "$MANAGER" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: Morphe Manager submodule is not initialized." >&2
  exit 1
fi

actual="$(git -C "$MANAGER" rev-parse HEAD)"
if [[ "$actual" != "$EXPECTED_MANAGER_SHA" ]]; then
  echo "ERROR: refusing to overlay unexpected Morphe revision: $actual" >&2
  exit 1
fi

if [[ ! -f "$SOURCE" || ! -f "$CATALOG_SOURCE" || ! -f "$TEST_SOURCE" || ! -f "$VALIDATOR_SOURCE" || ! -f "$VALIDATOR_TEST_SOURCE" || ! -f "$STORE_SOURCE" || ! -f "$MODULE" ]]; then
  echo "ERROR: VYRA overlay source or Morphe DI module is missing." >&2
  exit 1
fi

mkdir -p "$(dirname "$TARGET")"
cp "$SOURCE" "$TARGET"
cp "$CATALOG_SOURCE" "$CATALOG_TARGET"
cp "$VALIDATOR_SOURCE" "$VALIDATOR_TARGET"
mkdir -p "$(dirname "$TEST_TARGET")"
cp "$TEST_SOURCE" "$TEST_TARGET"
cp "$VALIDATOR_TEST_SOURCE" "$VALIDATOR_TEST_TARGET"
mkdir -p "$(dirname "$STORE_TARGET")"
cp "$STORE_SOURCE" "$STORE_TARGET"

python3 - "$MODULE" "$ROOT" "$MANAGER/app/build.gradle.kts" "$MANAGER/app/src/main/res/values/strings.xml" "$MANAGER/app/src/main/java/app/morphe/manager/ui/screen/shared/MainNavigation.kt" "$MANAGER/app/src/main/java/app/morphe/manager/ui/screen/HomeScreen.kt" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
root = Path(sys.argv[2])
app_gradle = Path(sys.argv[3])
strings_xml = Path(sys.argv[4])
main_navigation = Path(sys.argv[5])
home_screen = Path(sys.argv[6])
text = path.read_text(encoding="utf-8")
import_line = "import app.morphe.manager.domain.repository.VyraLocalBundleImporter"
registration = "    singleOf(::VyraLocalBundleImporter)"
catalog_registration = "    singleOf(::VyraCatalogRepository)"

if import_line not in text:
    anchor = "import app.morphe.manager.domain.repository.*\n"
    if anchor not in text:
        raise SystemExit("ERROR: unexpected Morphe RepositoryModule imports; refusing unsafe edit.")
    text = text.replace(anchor, anchor + import_line + "\n", 1)

if registration not in text:
    anchor = "    singleOf(::PatchBundleRepository)\n"
    if anchor not in text:
        raise SystemExit("ERROR: unexpected Morphe RepositoryModule bindings; refusing unsafe edit.")
    text = text.replace(anchor, anchor + registration + "\n", 1)

if catalog_registration not in text:
    if registration not in text:
        raise SystemExit("ERROR: VYRA importer binding is missing; refusing unsafe edit.")
    text = text.replace(registration, registration + "\n" + catalog_registration, 1)

path.write_text(text, encoding="utf-8")

# Add the VYRA Store as a first-class page in the existing engine navigation.
navigation_text = main_navigation.read_text(encoding="utf-8")
settings_entry = "    SETTINGS(R.string.settings, MaterialSymbols.Rounded.Settings)"
store_entry = "    VYRA_STORE(R.string.vyra_store, MaterialSymbols.Rounded.Apps)"
if store_entry not in navigation_text:
    if navigation_text.count(settings_entry) != 1:
        raise SystemExit("ERROR: MainPage enum changed; refusing unsafe Store navigation edit.")
    navigation_text = navigation_text.replace(settings_entry, settings_entry + ",\n" + store_entry, 1)
main_navigation.write_text(navigation_text, encoding="utf-8")

home_text_ui = home_screen.read_text(encoding="utf-8")
store_import = "import app.morphe.manager.ui.screen.vyra.VyraStorePage"
if store_import not in home_text_ui:
    anchor = "import app.morphe.manager.ui.screen.shared.AppPullToRefreshBox"
    if anchor not in home_text_ui:
        raise SystemExit("ERROR: HomeScreen imports changed; refusing unsafe Store page edit.")
    home_text_ui = home_text_ui.replace(anchor, store_import + "\n" + anchor, 1)

nav_covered_anchor = "                MainPage.SETTINGS -> false"
if "MainPage.VYRA_STORE -> false" not in home_text_ui:
    if home_text_ui.count(nav_covered_anchor) != 1:
        raise SystemExit("ERROR: HomeScreen navigationCovered cases changed; refusing unsafe Store page edit.")
    home_text_ui = home_text_ui.replace(nav_covered_anchor, nav_covered_anchor + "\n                MainPage.VYRA_STORE -> false", 1)

page_anchor = "            MainPage.APPS -> AppPullToRefreshBox("
if "MainPage.VYRA_STORE -> VyraStorePage" not in home_text_ui:
    if home_text_ui.count(page_anchor) != 1:
        raise SystemExit("ERROR: HomeScreen page cases changed; refusing unsafe Store page edit.")
    home_text_ui = home_text_ui.replace(page_anchor, """            MainPage.VYRA_STORE -> VyraStorePage(
                bottomBarHeight = bottomBarHeight,
                onAppSelected = { packageName -> homeViewModel.patchFlow.showPatchDialog(packageName) }
            )

""" + page_anchor, 1)
home_screen.write_text(home_text_ui, encoding="utf-8")

# Connect catalog discovery to the actual patch flow. Import is attempted only
# when Morphe has no source for this exact app version, and the repository's
# execution gate must approve the catalog entry before any MPP is imported.
patch_flow = root / "upstream/morphe-manager/app/src/main/java/app/morphe/manager/ui/viewmodel/PatchFlow.kt"
home_vm = root / "upstream/morphe-manager/app/src/main/java/app/morphe/manager/ui/viewmodel/HomeViewModel.kt"

flow_text = patch_flow.read_text(encoding="utf-8")
flow_dependency = "    private val vyraCatalogRepository: VyraCatalogRepository,\n"
flow_anchor = "    private val patchBundleRepository: PatchBundleRepository,\n"
if flow_dependency not in flow_text:
    if flow_anchor not in flow_text:
        raise SystemExit("ERROR: PatchFlow constructor changed; refusing unsafe VYRA integration.")
    flow_text = flow_text.replace(flow_anchor, flow_anchor + flow_dependency, 1)

flow_hook = """        // VYRA may supply a compatible source when Morphe has none for this exact APK.
        // Catalog entries remain blocked unless app status, publisher trust and execution policy
        // all explicitly permit import; network or catalog failures never stop normal patching.
        val existingForVersion = patchBundleRepository
            .scopedBundleInfoFlow(selectedApp.packageName, selectedApp.version, selectedApp.versionCode)
            .first()
        // scopedBundleInfoFlow includes empty per-source scopes, so checking list.isEmpty()
        // would incorrectly suppress catalog discovery whenever any source is installed.
        val hasUsableExistingPatches = existingForVersion.any {
            it.compatible.isNotEmpty() || it.universal.isNotEmpty()
        }
        val selectedVersion = selectedApp.version
        if (!hasUsableExistingPatches && !selectedVersion.isNullOrBlank()) {
            runCatching {
                val candidates = vyraCatalogRepository.findCompatible(selectedApp.packageName, selectedVersion)
                val eligible = candidates.filter { it.canImport }
                if (candidates.isNotEmpty() && eligible.isEmpty()) {
                    Log.i("PatchFlow", "VYRA found compatible catalog entries, but all are blocked by review policy")
                }
                eligible.forEach { candidate ->
                    vyraCatalogRepository.downloadAndImport(candidate, app.cacheDir)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.w("PatchFlow", "VYRA catalog lookup/import was skipped", error)
            }
        }

"""
flow_marker = "        // Read before the first selection is resolved, so every rule below and every later edit"
if "VYRA may supply a compatible source" not in flow_text:
    if flow_marker not in flow_text:
        raise SystemExit("ERROR: PatchFlow import point changed; refusing unsafe VYRA integration.")
    flow_text = flow_text.replace(flow_marker, flow_hook + flow_marker, 1)
patch_flow.write_text(flow_text, encoding="utf-8")

home_text = home_vm.read_text(encoding="utf-8")
# HomeViewModel is a KoinComponent. Resolve this dependency lazily instead of
# changing its constructor, which is also created by viewModelOf.
home_injection = "    private val vyraCatalogRepository: VyraCatalogRepository by inject()\n"
home_anchor = "    private val patcherProcesses: PatcherProcesses by inject()\n"
if home_injection not in home_text:
    if home_anchor not in home_text:
        raise SystemExit("ERROR: HomeViewModel Koin injection point changed; refusing unsafe VYRA integration.")
    home_text = home_text.replace(home_anchor, home_anchor + home_injection, 1)

patchflow_anchor = "        patchBundleRepository = patchBundleRepository,\n"
patchflow_dependency = "        vyraCatalogRepository = vyraCatalogRepository,\n"
patchflow_start = "    val patchFlow = PatchFlow(\n"
if patchflow_dependency not in home_text:
    start = home_text.find(patchflow_start)
    if start < 0:
        raise SystemExit("ERROR: HomeViewModel PatchFlow construction changed; refusing unsafe VYRA integration.")
    end = home_text.find("\n    )", start)
    if end < 0:
        raise SystemExit("ERROR: HomeViewModel PatchFlow call is incomplete; refusing unsafe VYRA integration.")
    block = home_text[start:end]
    if patchflow_anchor not in block:
        raise SystemExit("ERROR: PatchFlow repository argument changed; refusing unsafe VYRA integration.")
    block = block.replace(patchflow_anchor, patchflow_anchor + patchflow_dependency, 1)
    home_text = home_text[:start] + block + home_text[end:]
home_vm.write_text(home_text, encoding="utf-8")

# Keep the upstream application ID for the engine build because the pinned
# google-services.json contains Firebase clients only for app.morphe.manager
# and app.morphe.manager.debug. VYRA branding is applied through the launcher
# label; changing the package ID without an authorized Firebase client would
# break the build and could break Firebase-backed runtime features.
gradle_text = app_gradle.read_text(encoding="utf-8")
expected_application_id = 'applicationId = "app.morphe.manager"'
if gradle_text.count(expected_application_id) != 1:
    raise SystemExit("ERROR: unexpected Morphe applicationId declaration; refusing unsafe build identity edit.")

strings_text = strings_xml.read_text(encoding="utf-8-sig")
old_app_name = '<string name="app_name" translatable="false">Morphe</string>'
new_app_name = '<string name="app_name" translatable="false">VYRA Studio</string>'
store_string = '<string name="vyra_store">VYRA Store</string>'
if store_string not in strings_text:
    closing = "</resources>"
    if strings_text.count(closing) != 1:
        raise SystemExit("ERROR: unexpected strings.xml structure; refusing Store label edit.")
    strings_text = strings_text.replace(closing, "    " + store_string + "\n" + closing, 1)

if new_app_name not in strings_text:
    if strings_text.count(old_app_name) != 1:
        raise SystemExit("ERROR: unexpected Morphe app_name resource; refusing unsafe branding edit.")
    strings_text = strings_text.replace(old_app_name, new_app_name, 1)
strings_xml.write_text(strings_text, encoding="utf-8")

if expected_application_id not in app_gradle.read_text(encoding="utf-8"):
    raise SystemExit("ERROR: upstream Firebase-compatible applicationId was not preserved.")
if new_app_name not in strings_xml.read_text(encoding="utf-8"):
    raise SystemExit("ERROR: VYRA engine launcher label was not applied.")
PY

echo "Applied VYRA local MPP import bridge to pinned Morphe Manager."
