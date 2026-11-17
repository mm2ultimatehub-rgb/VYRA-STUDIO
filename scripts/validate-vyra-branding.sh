#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OVERLAY="$ROOT/integration/morphe-overlay"
MANAGER="$ROOT/upstream/morphe-manager"

required=(
  "$ROOT/branding/vyra-symbol.svg"
  "$OVERLAY/vyra_launcher_color.xml"
  "$OVERLAY/vyra_launcher_white.xml"
  "$OVERLAY/vyra_launcher_black.xml"
  "$OVERLAY/vyra_splash_theme.xml"
  "$ROOT/scripts/apply-vyra-morphe-overlay.sh"
)
for file in "${required[@]}"; do
  [[ -s "$file" ]] || { echo "ERROR: missing or empty branding asset: $file" >&2; exit 1; }
done

python3 - "$OVERLAY" "$ROOT/branding/vyra-symbol.svg" <<'PY'
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

overlay = Path(sys.argv[1])
svg = Path(sys.argv[2])
for name in (
    "vyra_launcher_color.xml",
    "vyra_launcher_white.xml",
    "vyra_launcher_black.xml",
    "vyra_splash_theme.xml",
):
    ET.parse(overlay / name)

root = ET.parse(svg).getroot()
if root.tag != "{http://www.w3.org/2000/svg}svg":
    raise SystemExit("ERROR: VYRA symbol source is not a valid SVG document.")
if root.attrib.get("viewBox") != "0 0 512 512":
    raise SystemExit("ERROR: VYRA SVG viewBox must remain 512×512.")
if not list(root):
    raise SystemExit("ERROR: VYRA SVG contains no vector artwork.")

theme = ET.parse(overlay / "vyra_splash_theme.xml").getroot()
items = {node.attrib.get("name"): (node.text or "").strip()
         for node in theme.iter("item")}
if items.get("windowSplashScreenAnimatedIcon") != "@drawable/ic_launcher_foreground":
    raise SystemExit("ERROR: native splash screen does not reference the VYRA mark.")
if items.get("windowSplashScreenBackground", "").upper() != "#080B1C":
    raise SystemExit("ERROR: unexpected VYRA splash background.")

for name in ("vyra_launcher_color.xml", "vyra_launcher_white.xml", "vyra_launcher_black.xml"):
    vector = ET.parse(overlay / name).getroot()
    if vector.tag != "{http://schemas.android.com/apk/res/android}vector":
        raise SystemExit(f"ERROR: {name} is not an Android vector drawable.")
    if vector.attrib.get("{http://schemas.android.com/apk/res/android}width") != "108dp":
        raise SystemExit(f"ERROR: {name} has an unexpected drawable width.")
    if not list(vector):
        raise SystemExit(f"ERROR: {name} has no vector paths.")

print("Validated VYRA SVG, launcher vector drawables, and native splash theme.")
PY

grep -q 'ic_launcher_foreground.xml' "$ROOT/scripts/apply-vyra-morphe-overlay.sh"
grep -q 'ic_launcher_foreground_white.xml' "$ROOT/scripts/apply-vyra-morphe-overlay.sh"
grep -q 'ic_launcher_foreground_black.xml' "$ROOT/scripts/apply-vyra-morphe-overlay.sh"
grep -q 'themes.xml' "$ROOT/scripts/apply-vyra-morphe-overlay.sh"

echo "VYRA branding overlay wiring checks passed."
