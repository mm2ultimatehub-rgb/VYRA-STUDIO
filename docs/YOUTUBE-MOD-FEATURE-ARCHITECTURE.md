# VYRA YouTube MOD — feature architecture

Status: design specification; not yet an executable patch module.

## Target

- Android package: `com.google.android.youtube`
- Initial local test APK: `21.30.216` (min API 29)
- Candidate upstream patch bundle: Morphe Patches `v2.3.0`
- Compatibility: the upstream patch source currently lists YouTube `21.30.216` as a supported target. This is a source declaration, not VYRA end-to-end verification.
- The original APK remains user-provided and local. Do not commit or publish it.

## Important discovery: glass navigation already exists upstream

The current Morphe YouTube patch source already contains a floating glass bottom-navigation implementation:

- `extensions/youtube/.../patches/YouTubeGlassNavigationBar.java`
- `extensions/youtube/.../patches/NavigationBarPatch.java`
- `patches/.../youtube/layout/buttons/navigation/NavigationBarPatch.kt`

It supports a rounded translucent capsule, adjustable opacity, height, width, corner roundness, button spacing, and selection-state updates. The upstream patch also has a navigation-bar settings surface. We should reuse and extend this existing implementation rather than create a competing glass renderer or duplicate upstream patch logic.

Upstream source references:
- https://github.com/MorpheApp/morphe-patches/blob/main/extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/YouTubeGlassNavigationBar.java
- https://github.com/MorpheApp/morphe-patches/blob/main/extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/NavigationBarPatch.java
- https://github.com/MorpheApp/morphe-patches/blob/main/patches/src/main/kotlin/app/morphe/patches/youtube/layout/buttons/navigation/NavigationBarPatch.kt
- https://github.com/MorpheApp/morphe-patches/blob/main/patches/src/main/kotlin/app/morphe/patches/youtube/shared/Constants.kt

## Desired user experience

### Default state

- YouTube keeps its familiar upstream interface.
- VYRA visual changes are not forced on the user.
- All VYRA-only appearance settings default to off.
- Existing upstream patch settings remain available, but VYRA MOD owns a single clear entry point for VYRA-specific options.

### Entry point

YouTube Settings → VYRA MOD

The VYRA MOD page groups settings into:
1. Appearance
2. Navigation
3. Motion
4. Player
5. Advanced
6. Reset

### First release controls

| Key | Label | Default | Behavior |
|---|---|---:|---|
| `vyra_mod_enabled` | VYRA MOD | false | Master switch for VYRA-specific UI changes |
| `vyra_glass_navigation` | Liquid Glass navigation | false | Enables the upstream glass capsule renderer, configured with VYRA defaults |
| `vyra_animated_navigation` | Animated navigation | false | Enables a restrained selection transition and icon state animation where supported |
| `vyra_glass_opacity` | Glass opacity | 20 | Maps to a validated 0–100 opacity range |
| `vyra_glass_roundness` | Glass roundness | 30 | Selects the capsule corner profile |
| `vyra_accent_color` | Accent color | system | Uses system/default color until the user chooses a supported preset |
| `vyra_player_style` | Player appearance | default | No player change until a supported style is selected |

The master switch must dominate all VYRA settings. If it is off, VYRA-only changes must not be applied. Each feature must also be independently disableable.

### Liquid Glass behavior

When enabled, the navigation bar should:
- float as a translucent rounded capsule;
- preserve page content behind the capsule and handle system gesture/navigation insets;
- keep button labels, accessibility descriptions and touch targets usable;
- animate selected-tab changes without excessive bounce or latency;
- respect Android reduced-motion / animator-duration settings;
- avoid blur fallbacks that cause excessive GPU or battery cost;
- degrade gracefully on Android versions/devices without the required blur behavior.

When disabled, restore the normal upstream navigation appearance. Do not change unrelated YouTube screens.

## Implementation boundaries

- Keep upstream Morphe patches intact and separately attributed.
- Add VYRA-only patches in a separate package/module and give them unique setting keys.
- Reuse upstream glass drawing/layout helpers where the API and license permit; do not copy/paste a second implementation.
- The VYRA MOD settings page is a separate extension/settings feature, not a replacement for YouTube's own settings.
- Do not alter account, sign-in, subscription, playback entitlement, ads, or payment behavior as part of this visual feature.
- Treat each YouTube app version as a separate compatibility target. A patch match is not proof that the resulting APK works.

## Release gates

1. Inspect and pin the exact upstream patch release and source commit.
2. Review upstream LICENSE, NOTICE, dependencies and all bundled assets.
3. Implement the VYRA MOD settings entry and preference storage.
4. Wire the VYRA Liquid Glass switch to the existing glass navigation implementation.
5. Add animation only after checking upstream selection callbacks and lifecycle behavior.
6. Build the patch bundle and apply it to the local 21.30.216 APK.
7. Verify output signature, install, launch, sign-in, feed, navigation, video playback, rotation, keyboard, gesture navigation, 3-button navigation, reduced motion, and reset.
8. Test master switch off and each individual feature off.
9. Publish source, notices, version compatibility and output hashes. Do not distribute proprietary YouTube APK files.

## Honest current status

The upstream patch source already contains a glass navigation implementation, and 21.30.216 is listed among its supported targets. VYRA's own MOD settings hub, VYRA-specific animations, combined patch/build/sign flow, and device verification have not yet been implemented. This document does not claim a finished APK.
