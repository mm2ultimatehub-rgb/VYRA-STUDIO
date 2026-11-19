# VYRA MOD integration map — YouTube 21.30.216

Status: source-level integration design; not yet wired into a patcher build.

## Verified upstream extension points

Pinned source: Morphe Patches 2.3.0, commit `31234a8465683a389bcfba25ad636fd1cd38fd02`.

| Capability | Existing source | Existing preference key |
|---|---|---|
| Liquid Glass navigation capsule | `extensions/youtube/.../patches/YouTubeGlassNavigationBar.java` | `morphe_navigation_bar_glass` |
| Glass opacity | `extensions/youtube/.../settings/Settings.java` | `morphe_navigation_bar_glass_opacity` |
| Glass roundness | same | `morphe_navigation_bar_glass_round` |
| Glass height / width / spacing | `YouTubeGlassNavigationBarPreference.java` + `Settings.java` | `morphe_navigation_bar_glass_height`, `..._width`, `..._button_spacing` |
| Navigation animations | `NavigationBarPatch.java` | `morphe_navigation_bar_animations` |

The patch DSL currently registers the navigation preferences in the existing General screen from:
`patches/src/main/kotlin/app/morphe/patches/youtube/layout/buttons/navigation/NavigationBarPatch.kt`.

The extension settings UI is built by the existing settings patch and displayed by
`YouTubePreferenceFragment.java`. The fragment already supports nested preference screens,
so a VYRA entry can be presented as a normal YouTube settings destination.

## Required VYRA integration change

Do not add a second copy of the same SharedPreferences controls while leaving the original
Morphe navigation screen active. That would create duplicate controls and a confusing settings
experience.

Instead, in the pinned upstream patch source, refactor the existing navigation preference
collection into one reusable set and mount it under a VYRA-owned screen:

1. Add a top-level preference screen with key `vyra_mod_screen`, title key
   `vyra_mod_title`, and VYRA icon.
2. Move the existing navigation appearance controls into a VYRA-owned sub-screen/category,
   retaining the original Morphe preference keys so the upstream runtime renderer continues
   to read the exact same values.
3. Keep non-appearance navigation controls in the normal General settings area.
4. Add a dedicated VYRA appearance page for Glass and motion. Default both Glass and
   navigation animations to OFF; preserve the upstream defaults for opacity, size and spacing.
5. Add localized string resources for the VYRA title, summary and appearance section.
6. Ensure the original navigation screen no longer registers duplicate Glass/motion controls.

## Compatibility and release gates

- Target package: `com.google.android.youtube`
- Target version: `21.30.216`
- Upstream compatibility declaration is not an end-to-end VYRA build verification.
- Build the pinned patcher + patches + extension from source, apply to the user-provided APK,
  sign with a VYRA test/release key, then install and verify on Android.
- Verify clean install, upgrade behavior, settings persistence, Glass ON/OFF, animations ON/OFF,
  light/dark themes, gesture navigation and three-button navigation.
- Preserve GPL-3.0 notices and corresponding source obligations before distributing derivative builds.

## Explicitly not complete

This file is an integration map, not executable patch code. The VYRA repository currently
does not contain the pinned upstream patcher/extension source tree or a configured build
pipeline. No APK has been patched, signed or device-tested by this change.
