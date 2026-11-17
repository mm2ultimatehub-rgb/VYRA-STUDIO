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
TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/VyraLocalBundleImporter.kt"
CATALOG_TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/VyraCatalogRepository.kt"
TEST_TARGET="$MANAGER/app/src/test/java/app/morphe/manager/domain/repository/VyraCatalogRepositoryTest.kt"
VALIDATOR_TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/MppArchiveValidator.kt"
VALIDATOR_TEST_TARGET="$MANAGER/app/src/test/java/app/morphe/manager/domain/repository/MppArchiveValidatorTest.kt"
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

if [[ ! -f "$SOURCE" || ! -f "$CATALOG_SOURCE" || ! -f "$TEST_SOURCE" || ! -f "$VALIDATOR_SOURCE" || ! -f "$VALIDATOR_TEST_SOURCE" || ! -f "$MODULE" ]]; then
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

python3 - "$MODULE" "$ROOT" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
root = Path(sys.argv[2])
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
        val selectedVersion = selectedApp.version
        if (existingForVersion.isEmpty() && !selectedVersion.isNullOrBlank()) {
            runCatching {
                vyraCatalogRepository.findCompatible(selectedApp.packageName, selectedVersion)
                    .filter { it.canImport }
                    .forEach { candidate ->
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
PY

echo "Applied VYRA local MPP import bridge to pinned Morphe Manager."
