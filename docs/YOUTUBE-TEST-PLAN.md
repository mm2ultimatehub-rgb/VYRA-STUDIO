# YouTube test APK and patch compatibility

Research date: 2026-11-17

## Test input

The user supplied an APK named:

`com.google.android.youtube_21.30.216-1561187872_minAPI29(arm64-v8a,armeabi-v7a,x86,x86_64)(nodpi)_apkmirror.com.apk`

The archive was inspected as an Android APK. The filename identifies it as YouTube package `com.google.android.youtube`, version `21.30.216`, minimum API 29, with the listed ABI variants. The filename is not proof of publisher authenticity. VYRA must inspect the package metadata and signing certificate from the actual file before any patch run.

## Upstream compatibility finding

The Morphe Patches README on its current `main` branch lists YouTube version `21.30.216` in the supported-version list for the YouTube patch collection. The README currently lists 118 YouTube patches in that section.

Important qualification: a version appearing in the collection's supported-version list is a useful compatibility signal, not a guarantee that every individual patch works with every APK variant or signer. Each selected patch still needs its own compatibility/fingerprint checks and a real test run.

Source: https://github.com/MorpheApp/morphe-patches/blob/main/README.md

## What the upstream engine does

Morphe Patcher documentation describes patch code that can modify:
- Dalvik VM bytecode (DEX);
- decoded Android resources and resource tables;
- arbitrary files inside the APK.

Patches are modular, may declare dependencies, and execute against a patch context. The engine lazily decodes DEX/resources/files when a patch first needs them. This is fundamentally more involved than downloading a patch bundle: VYRA currently downloads and SHA-256-verifies an artifact but does not execute it or rewrite an APK.

Sources:
- https://github.com/MorpheApp/morphe-patcher
- https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_patches_intro.md

## Patch bundle and source trust

Morphe Manager documents local `.mpp` bundles and remote GitHub/GitLab sources. It warns that patch sources determine what code is incorporated into modified apps and should only be added when trusted.

VYRA's repository index v1 is currently its own declarative catalog format. It is not the Morphe `.mpp` format and must not be treated as executable patch code. A future adapter would need to explicitly support a reviewed upstream bundle format and its runtime requirements.

Source: https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md

## License and branding review

Morphe Patches and Morphe Patcher use GPL-3.0 with additional Section 7 conditions. The Patches NOTICE explicitly requires preservation of user notices for derivative works, prohibits misrepresenting origin, requires modified versions to be clearly distinguished, and grants no trademark permission for the Morphe name or marks.

Before integrating or redistributing upstream engine or patch code, VYRA must complete a component-by-component license review, preserve required notices, provide corresponding source where GPL requires it, and keep VYRA branding distinct. This document is research, not a legal opinion.

Sources:
- https://github.com/MorpheApp/morphe-patches/blob/main/LICENSE
- https://github.com/MorpheApp/morphe-patches/blob/main/NOTICE
- https://github.com/MorpheApp/morphe-patcher/blob/main/README.md

## VYRA test sequence

1. Import the user-provided APK through Android's document picker.
2. Record package name, version code/name, min/target SDK, APK SHA-256, and signer certificate SHA-256.
3. Query the saved patch catalog and show only package/version-compatible entries.
4. Do not label an upstream patch compatible merely because the app version is listed; show the upstream version evidence and per-patch constraints separately.
5. Before engine integration, decide whether VYRA will use a GPL-compatible integration/fork or implement an independently designed engine. Do not copy upstream patch source into VYRA without the license review.
6. For the first real patch test, use a reversible, low-risk patch on a user-authorized APK; preserve the original and verify the output APK signature and installability.
7. Do not distribute Google's original APK or claim the output is an official YouTube build.

## Current VYRA capability boundary

Implemented: APK selection/inspection, repository index parsing, package/version compatibility filtering, HTTPS artifact download, SHA-256 verification, and app-private artifact storage.

Not implemented yet: execution of `.mpp` bundles, DEX/resource rewriting, APK rebuilding, APK signing, and installation/export of a patched APK.

## Published upstream bundle candidate

The GitHub Releases API currently identifies Morphe Patches `v2.3.0` (published 2026-11-16) and exposes the asset `patches-2.3.0.mpp`:

- Size: 16,663,050 bytes
- Published SHA-256: `1c37549274a2b6cf7f2e3e099d9b8cb641880b8a7dac323a95638a06bb4509a9`
- Official release: https://github.com/MorpheApp/morphe-patches/releases/tag/v2.3.0
- Asset: https://github.com/MorpheApp/morphe-patches/releases/download/v2.3.0/patches-2.3.0.mpp

The bundle is smaller than VYRA's current 50 MB artifact-download limit and its published digest can be used for an integrity check. The bundle has not been downloaded or executed by VYRA in this research step. Its release asset is a multi-app patch collection, so it must not be misrepresented as a single YouTube-only patch. Runtime support and licensing still need review before use.
