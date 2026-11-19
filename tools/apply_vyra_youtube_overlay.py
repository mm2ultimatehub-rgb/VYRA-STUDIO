#!/usr/bin/env python3
"""Apply the VYRA YouTube settings overlay to the pinned Morphe Patches checkout."""
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
old_group = '''        navPreferences += listOf(
            SwitchPreference("morphe_navigation_bar_animations", summary = true),
            ListPreference("morphe_navigation_bar_style"),
            SwitchPreference("morphe_navigation_bar_glass", summary = true),
            NonInteractivePreference(
                key = "morphe_navigation_bar_glass_opacity",
                tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference"
            ),
            SwitchPreference("morphe_navigation_bar_glass_round", summary = true),
            // Height, width and button spacing, with a preview.
            NonInteractivePreference(
                key = "morphe_navigation_bar_glass_height",
                titleKey = "morphe_navigation_bar_glass_size_title",
                summaryKey = "morphe_navigation_bar_glass_size_summary",
                tag = "app.morphe.extension.youtube.settings.preference.YouTubeGlassNavigationBarPreference",
                selectable = true
            )
        )'''
new_group = '''        // Keep the stock navigation style selector in General. VYRA appearance
        // controls are registered on their own screen below.
        navPreferences += listOf(
            ListPreference("morphe_navigation_bar_style")
        )'''
if source.count(old_group) != 1:
    fail("the pinned NavigationBarPatch preference block changed; refusing a partial edit")
source = source.replace(old_group, new_group, 1)

anchor = '''        )

        // Swap create with notifications button.'''
addition = '''        )

        // VYRA-owned appearance screen. Keep original preference keys because the
        // upstream Glass navigation renderer reads these exact values.
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

        // Swap create with notifications button.'''
if source.count(anchor) != 1:
    fail("navigation settings insertion anchor changed; review upstream before applying")
source = source.replace(anchor, addition, 1)
NAV.write_text(source, encoding="utf-8")

resources = ROOT / "patches/src/main/resources/addresources/values/youtube/strings.xml"
if not resources.is_file():
    fail(f"missing pinned YouTube string resources: {resources}")
xml = resources.read_text(encoding="utf-8")
xml, count = re.subn(
    r'<string\s+name="morphe_settings_title"[^>]*>.*?</string>',
    '<string name="morphe_settings_title">VYRA MOD</string>',
    xml,
    count=1,
    flags=re.DOTALL,
)
if count != 1:
    fail("could not rename the existing visible settings title")
if 'name="vyra_appearance_title"' not in xml:
    xml, count = re.subn(
        r"</resources>\s*$",
        '    <string name="vyra_appearance_title">VYRA Appearance</string>\n</resources>\n',
        xml,
    )
    if count != 1:
        fail("could not append the VYRA Appearance title resource")
resources.write_text(xml, encoding="utf-8")

print("VYRA overlay applied: root title and appearance preference group updated.")
