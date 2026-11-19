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
            SwitchPreference("morphe_hide_navigation_new_content_dot"),
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
new_group = '''        // Keep ordinary navigation controls in General. VYRA appearance controls
        // are registered on their own screen below.
        navPreferences += listOf(
            SwitchPreference("morphe_hide_navigation_new_content_dot"),
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

shared_strings = ROOT / "patches/src/main/resources/addresources/values/shared/strings.xml"
youtube_strings = ROOT / "patches/src/main/resources/addresources/values/youtube/strings.xml"
for path in (shared_strings, youtube_strings):
    if not path.is_file():
        fail(f"missing pinned resource file: {path}")

shared_xml = shared_strings.read_text(encoding="utf-8")
shared_xml, count = re.subn(
    r'<string\s+name="morphe_settings_title"[^>]*>.*?</string>',
    '<string name="morphe_settings_title" translatable="false">VYRA MOD</string>',
    shared_xml,
    count=1,
    flags=re.DOTALL,
)
if count != 1:
    fail("could not rename the visible root settings title")
shared_strings.write_text(shared_xml, encoding="utf-8")

youtube_xml = youtube_strings.read_text(encoding="utf-8")
if 'name="vyra_appearance_title"' not in youtube_xml:
    youtube_xml, count = re.subn(
        r"</resources>\s*$",
        '    <string name="vyra_appearance_title">VYRA Appearance</string>\n</resources>\n',
        youtube_xml,
    )
    if count != 1:
        fail("could not append the VYRA Appearance title resource")
    youtube_strings.write_text(youtube_xml, encoding="utf-8")

print("VYRA overlay applied: visible root title renamed and Glass/motion controls grouped under VYRA Appearance.")
