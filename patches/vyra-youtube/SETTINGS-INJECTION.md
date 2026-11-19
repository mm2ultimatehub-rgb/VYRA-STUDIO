# VYRA YouTube settings injection — source-level implementation notes

Pinned source: Morphe Patches 2.3.0, commit
`31234a8465683a389bcfba25ad636fd1cd38fd02`.

Target: YouTube `21.30.216` (`com.google.android.youtube`).

## Verified injection points

### 1. YouTube Settings root

The upstream `patches/src/main/kotlin/app/morphe/patches/youtube/misc/settings/SettingsPatch.kt`
creates a `settingsResourcePatch` and calls the shared `settingsPatch(...)` DSL with
`rootPreferences`. The current Morphe entry is injected into both `settings_fragment`
and `settings_fragment_cairo` using an `IntentPreference` / `PreferenceCategory`.

This is the right integration point for a VYRA-owned root entry. It should use its own
resource keys, icon drawables, intent action and Java/Kotlin extension classes. Do not
rename only the visible Morphe string: the current intent, resources, settings fragment,
and extension initialization are coupled.

### 2. Nested preference screens

The navigation patch currently builds a `navPreferences` collection and registers it as
`PreferenceScreen.GENERAL.addPreferences(PreferenceScreenPreference(...))`. The screen
contains the existing navigation controls and the Glass controls.

The Glass-related settings are:
- `morphe_navigation_bar_animations`
- `morphe_navigation_bar_glass`
- `morphe_navigation_bar_glass_opacity`
- `morphe_navigation_bar_glass_round`
- `morphe_navigation_bar_glass_height`

The height preference opens a custom preview/size control that also edits width and button
spacing. Keep these exact keys when relocating the controls: the existing runtime renderer
reads them.

### 3. Runtime implementation

The YouTube extension owns `YouTubeGlassNavigationBar.java` and
`NavigationBarPatch.java`. These classes implement the capsule rendering and the hooks
injected into YouTube's navigation bar. The VYRA work should reuse these classes from the
pinned upstream source, not create a second overlay renderer.

## Intended VYRA settings tree

YouTube Settings
└── VYRA MOD
    ├── Appearance
    │   ├── Liquid Glass (OFF by default)
    │   ├── Glass opacity
    │   ├── Rounded capsule
    │   ├── Capsule size and spacing
    │   └── Navigation animations (OFF by default)
    ├── Player
    │   └── Future player appearance controls
    └── About VYRA
        └── Version, credits, source and license notices

The Appearance screen is the first implementation target. Player and About pages must not
show fake controls: add them only when their runtime behavior and localized resources exist.

## Implementation sequence

1. Add VYRA root-entry strings, icon resources, intent action, and extension destination.
2. Register VYRA in both legacy and Cairo YouTube settings layouts, matching the upstream
   root-entry pattern.
3. Move the existing navigation appearance preferences into the VYRA Appearance screen,
   retaining their existing preference keys and custom preference class.
4. Remove the old duplicate Glass/animation controls from the General navigation screen.
5. Keep unrelated navigation controls in General.
6. Add VYRA About/legal attribution and preserve all upstream LICENSE/NOTICE obligations.
7. Build the exact target APK, inspect the patch log, install on a test device, and verify
   Liquid Glass and animation toggles in both light and dark themes.

## Current completion status

This document records verified source extension points and the implementation design. It is
not a compiled patch. No VYRA settings entry has yet been injected into YouTube, no APK has
been rebuilt or signed, and no device test has been performed.
