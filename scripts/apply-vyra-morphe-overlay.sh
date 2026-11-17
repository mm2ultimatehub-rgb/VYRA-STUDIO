#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANAGER="$ROOT/upstream/morphe-manager"
EXPECTED_MANAGER_SHA="d9f56746d13890c0f8837c6d070dae6f210ad2a9"
SOURCE="$ROOT/integration/morphe-overlay/VyraLocalBundleImporter.kt"
TARGET="$MANAGER/app/src/main/java/app/morphe/manager/domain/repository/VyraLocalBundleImporter.kt"
MODULE="$MANAGER/app/src/main/java/app/morphe/manager/di/RepositoryModule.kt"

if [[ ! -d "$MANAGER/.git" ]]; then
  echo "ERROR: Morphe Manager submodule is not initialized." >&2
  exit 1
fi

actual="$(git -C "$MANAGER" rev-parse HEAD)"
if [[ "$actual" != "$EXPECTED_MANAGER_SHA" ]]; then
  echo "ERROR: refusing to overlay unexpected Morphe revision: $actual" >&2
  exit 1
fi

if [[ ! -f "$SOURCE" || ! -f "$MODULE" ]]; then
  echo "ERROR: VYRA overlay source or Morphe DI module is missing." >&2
  exit 1
fi

mkdir -p "$(dirname "$TARGET")"
cp "$SOURCE" "$TARGET"

python3 - "$MODULE" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
import_line = "import app.morphe.manager.domain.repository.VyraLocalBundleImporter"
registration = "    singleOf(::VyraLocalBundleImporter)"

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

path.write_text(text, encoding="utf-8")
PY

echo "Applied VYRA local MPP import bridge to pinned Morphe Manager."
