# Patcher Runtime Adoption — Verified Integration Contract

Status: source-level integration specification; no runtime code has been copied or wired into VYRA yet.

Pinned upstream baseline:
- Morphe Manager: `d9f56746d13890c0f8837c6d070dae6f210ad2a9`
- Morphe Patcher: `0aed87c4e3264cf9233feab7ed26cf2543a08b96`
- Morphe Patches: `31234a8465683a389bcfba25ad636fd1cd38fd02`

## Why adding one Maven dependency is not the integration

The pinned Manager does not consume its patching runtime as a single standalone Android artifact. It includes a local `:patcher-core` module, and `settings.gradle.kts` conditionally includes the sibling `morphe-patcher` source build. The Android app also owns the Android-specific patch bundle loader, device configuration, session orchestration, signing/export flow, and patch option persistence.

The `morphe-patcher` project itself is a JVM library. Its README describes it as the library powering Manager/Desktop, but the Manager's Android runtime contract is implemented across the app and `:patcher-core`. Therefore VYRA must adopt the compatible source modules and required notices as a coherent baseline, not add an arbitrary dependency and claim that APK patching works.

## Verified execution contract

The pinned Manager's `patcher-core` exposes these core types:

- `PatchRequest`: package name, input/output paths, temporary/workspace paths, selected bundles, per-patch JSON option values, native-library stripping choice, and device configuration.
- `PatchPipeline`: receives a `PatchRequest`, logger, listener, and a suspend `loadPatches(bundle)` callback; `run()` resolves selected patches and invokes the patch session.
- `Session`: configures `Patcher`, applies selected patches, writes an aligned unsigned APK, and reports progress/results through `PatchingListener`.
- Android app's `PatchBundle.Loader.patches(bundle)`: validates the bundle's DEX entries and loads patch implementations from DEX into the Android runtime.

The pipeline output is explicitly an **unsigned** APK. A separate signing and verification stage is mandatory before export or installation.

## VYRA adoption sequence

1. **Baseline import:** bring in the pinned Manager app, `:patcher-core`, Morphe Patcher source build, and the exact transitive source/dependency setup required by the pinned commits. Preserve GPL-3.0 and all applicable upstream NOTICE/Section 7 terms.
2. **Branding boundary:** replace only product-facing identity, icons, theme, navigation and VYRA-owned screens. Keep package patching internals intact until the fork builds and passes its baseline tests.
3. **Runtime bridge:** adapt VYRA's APK selection and patch catalogue to construct the verified `PatchRequest`; use the Android DEX loader for trusted patch bundles; connect cancellation, progress, logs, and failures to the UI.
4. **Signing/export:** reuse the upstream signing path, keep signing material private, verify the resulting APK signature and package identity, and export only after validation.
5. **YouTube patch profile:** apply the VYRA YouTube-specific source modifications to the pinned YouTube extension/patches. The profile targets `com.google.android.youtube` version `21.30.216`; upstream compatibility metadata is not proof of a successful VYRA build.
6. **VYRA settings injection:** add a separate VYRA root settings entry and move the existing glass-navigation/motion preferences beneath it while retaining their established preference keys and renderer.
7. **Verification:** build from a clean checkout; run unit tests; patch an authorized local APK; verify signature/install/launch; verify Glass and motion toggles in both states and test light/dark plus gesture/three-button navigation.

## Non-negotiable release gates

- No release claim until the patched YouTube APK has actually been produced by the integrated runtime.
- No fake settings for features that have no runtime behavior.
- Default Glass and navigation animations remain off.
- Do not commit the user's original YouTube APK, signing keys, or private credentials.
- Do not publish a modified proprietary APK. Distribute source/patches and user-built artifacts only where the relevant rights and license conditions permit.

## Current VYRA repository state

The current VYRA-STUDIO branch has APK inspection, repository metadata/download, MPP structure inspection, Compose UI, tests, and a shell CI build. It does **not** yet contain the Manager patching runtime, DEX patch execution, APK rebuild/signing, or the YouTube settings injection. A green shell CI run is not a patched-APK verification.
