# VYRA YouTube MOD — implementation plan

## Product behavior

The target is a YouTube build that behaves exactly like the selected upstream YouTube release until the user explicitly enables VYRA MOD in YouTube Settings.

- Default state: preserve the upstream YouTube experience; do not force a custom theme or navigation.
- Entry point: YouTube Settings → VYRA MOD.
- VYRA MOD hub: grouped controls for appearance, navigation, animations, player and advanced options.
- Liquid Glass: opt-in preset. When enabled, apply a coordinated glass-style bottom navigation, animated selection indicator, icon transitions, translucent surfaces and matching motion to supported UI elements.
- Every visual feature must have an independent toggle and a safe reset-to-default action.
- Turning VYRA MOD off restores the normal upstream appearance.
- Persist preferences locally and apply them on restart without requiring a reinstall.

## Initial feature set

1. VYRA MOD settings entry and hub.
2. Liquid Glass preset (off by default).
3. Animated bottom navigation with accessible reduced-motion fallback.
4. Icon style and accent color controls.
5. Player appearance controls.
6. Reset all VYRA settings.
7. Compatibility/version metadata and a clear unsupported-version state.

## Target and inputs

- Target package: com.google.android.youtube
- Initial APK candidate: 21.30.216 (user-supplied; do not mirror or redistribute).
- Patch catalog: VYRA-REPOSITORY, official Morphe source listed as candidate.
- The matching APK is an input for local testing only. It must not be committed to a public repository or included in public release assets.

## Engineering approach

- Keep the YouTube base app and patch bundles separate from VYRA's own patch modules.
- Inspect upstream patch bundle metadata and licenses before execution.
- Use a pinned, reviewed patch-engine baseline; preserve required upstream attribution, notices, corresponding source and license conditions.
- Implement VYRA-specific behavior as separately versioned patch modules where the engine supports it.
- Do not claim compatibility from a catalog entry alone. Verify package/version, patch dependencies, output signature, installation and smoke tests.
- Never overwrite or modify the only copy of the source APK. Keep original and output artifacts separate.

## Release gates

A build is releasable only after:
- clean reproducible build;
- APK signature and package metadata validation;
- installation on a supported Android device;
- default YouTube experience smoke test;
- VYRA MOD entry and every toggle tested;
- Liquid Glass navigation and animation tested;
- disable/reset returns to the default appearance;
- crash-free playback, navigation, sign-in and settings smoke tests;
- source, notices, dependency licenses and artifact hashes recorded.

## Current known blockers

The current VYRA-STUDIO main branch is a Kotlin/Compose patch-workspace shell. Its own README states that it does not yet execute patch bundles, rebuild or sign modified APKs. The saved Morphe Manager debug APK is a useful reference/runtime artifact, not source code that can simply be merged into the YouTube APK. The next engineering milestone is to establish a reproducible, license-compliant patch-engine source baseline before producing the customized YouTube build.
