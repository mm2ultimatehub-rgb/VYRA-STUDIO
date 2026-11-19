#!/usr/bin/env python3
"""Apply the VYRA YouTube settings overlay to the pinned Morphe Patches checkout.

This deliberately edits source files, not a prebuilt APK. It fails closed when the
pinned upstream layout changes so CI cannot silently produce an unmodified bundle.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
NAV = ROOT / "patches/src/main/kotlin/app/morphe/patches/youtube/layout/buttons/navigation/NavigationBarPatch.kt"

def fail(message: str) -> None:
    print(f"VYRA overlay error: {message}", file=sys.stderr)
    raise SystemExit(2)

if not NAV.is_file():
    fail(f"missing upstream source: {NAV}")

source = NAV.read_text(encoding="utf-8")
required = [
    'SwitchPreference("morphe_navigation_bar_animations", summary = true),',
    'SwitchPreference("morphe_navigation_bar_glass", summary = true),',
    'key = "morphe_navigation_bar_glass_opacity",',
    'SwitchPreference("morphe_navigation_bar_glass_round", summary = true),',
    'key = "morphe_navigation_bar_glass_height",',
]
for token in required:
    if source.count(token) != 1:
        fail(f"expected exactly one upstream anchor, got {source.count(token)}: {token}")

# Remove VYRA appearance controls from the generic Navigation bar screen.
for token in required:
    lines = source.splitlines(keepends=True)
    source = "".join(line for line in lines if token not in line)

# Keep the original renderer and preference keys; only regroup its controls.
anchor = """        )

        // Swap create with notifications button."""
addition = """        )

        // VYRA-owned appearance screen. The original Morphe preference keys are
        // intentionally retained because the upstream Glass renderer reads them.
        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "vyra_appearance_screen",
                titleKey = "vyra_appearance_title",
                sorting = Sorting.UNSORTED,
                preferences = mutableSetOf(
                    SwitchPreference("morphe_navigation_bar_animations", summary = true),
                    SwitchPreference("morphe_navigation_bar_glass", summary = true),
                    NonInteractivePreference(
                        key = "morphe_navigation_bar_glass_opacity",
                        tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference"
                    ),
                    SwitchPreference("morphe_navigation_bar_glass_round", summary = true),
                    NonInteractivePreference(
                        key = "morphe_navigation_bar_glass_height",
                        titleKey = "morphe_navigation_bar_glass_size_title",
                        summaryKey = "morphe_navigation_bar_glass_size_summary",
                        tag = "app.morphe.extension.youtube.settings.preference.YouTubeGlassNavigationBarPreference",
                        selectable = true
                    )
                )
            )
        )

        // Swap create with notifications button."""
if source.count(anchor) != 1:
    fail("navigation settings insertion anchor changed; review upstream before applying")
source = source.replace(anchor, addition, 1)
NAV.write_text(source, encoding="utf-8")

# Add the VYRA page title to the upstream translated string resources.
resource_files = list((ROOT / "extensions/youtube/src/main/resources").rglob("strings.xml"))
target = None
for path in resource_files:
    text = path.read_text(encoding="utf-8")
    if 'name="morphe_settings_title"' in text:
        target = path
        break
if target is None:
    fail("could not find the YouTube morphe_settings_title string resource")

xml = target.read_text(encoding="utf-8")
if 'name="vyra_appearance_title"' not in xml:
    xml = re.sub(
        r"</resources>\\s*$",
        '    <string name="vyra_appearance_title">VYRA Appearance</string>\\n</resources>\\n',
        xml,
    )
    if 'name="vyra_appearance_title"' not in xml:
        fail(f"could not append VYRA title resource to {target}")
target.write_text(xml, encoding="utf-8")

print("VYRA source overlay applied: Glass + navigation motion controls now live on the VYRA Appearance screen.")
