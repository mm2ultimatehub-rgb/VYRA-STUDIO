# Upstream research: Android patching ecosystem

Research snapshot: 2026-11-17

## Morphe Patcher

Repository: https://github.com/MorpheApp/morphe-patcher

The upstream README describes Morphe Patcher as a library used by Morphe Desktop, Morphe Manager, and third-party patch managers. Its advertised capabilities include Dalvik bytecode disassembly/assembly, APK resource decoding/building, arbitrary APK file read/write, and modular patch APIs.

License: GPL-3.0 with additional GPLv3 Section 7 conditions. The upstream README specifically calls out distinct derivative branding and prohibits use of Morphe branding for derivative products. Read the repository LICENSE and NOTICE files before any reuse or redistribution.

## Morphe Patches

Repository: https://github.com/MorpheApp/morphe-patches

The upstream README describes the patch collection as based on prior ReVanced work. It publishes patch lists and supported target-app versions. It is licensed GPL-3.0 with additional Section 7 conditions, including preservation of notices and distinct branding.

## Patch bundle workflow

Morphe documentation describes patch bundles with the `.mpp` extension. The desktop tool can load local bundles or supported GitHub/GitLab repository sources. The official template documents a Gradle `buildAndroid` task that produces a `.mpp` artifact under `patches/build/libs/`.

This is useful evidence for the ecosystem's bundle workflow, but it does not prove that every desktop feature or artifact can be consumed directly by an Android app. Android runtime compatibility must be tested separately.

## Repository trust and safety

A patch source is executable modification logic, not merely a passive catalog. The app must:
- show the source owner and origin;
- require explicit confirmation before adding an external source;
- distinguish trusted, user-added, and unverified sources;
- show supported package names and app versions;
- validate downloaded artifact hashes where available;
- never silently execute a newly added source;
- keep APKs, signing keys, tokens, and private build outputs out of Git.

## VYRA implementation rule

For the first milestone, implement the UI and typed contracts only. Do not vendor Morphe code or distribute upstream APKs. Engine integration requires a separate license and technical review, followed by tests against user-provided APKs that the user is authorized to modify.

## Sources

- Morphe Patcher: https://github.com/MorpheApp/morphe-patcher
- Morphe Patches: https://github.com/MorpheApp/morphe-patches
- Morphe Manager patch sources: https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md
- Morphe Desktop documentation: https://github.com/MorpheApp/morphe-desktop/blob/main/docs/documentation.md
- Morphe patch template: https://github.com/MorpheApp/morphe-patches-template


## YouTube test target finding (2026-11-17)

The current Morphe Patches README lists YouTube 21.30.216 among the supported versions for its YouTube patch collection and lists 118 YouTube patches in that section. This matches the version in the user's supplied APK filename. This is collection-level compatibility evidence only; it does not guarantee every patch is compatible with every APK build or signing certificate. See [YouTube test plan](YOUTUBE-TEST-PLAN.md).

The Patches NOTICE was inspected directly. It requires preservation of the notice for derivative works, clear identification of modified versions to avoid origin confusion, and states that the GPL license grants no trademark permission for the Morphe name or marks.

The upstream patch documentation says patches may modify DEX bytecode, decoded resources/resource tables, and arbitrary APK files. It also describes patch dependencies and lazy decoding. This confirms that a real patch engine is a substantial APK transformation subsystem, not just a bundle downloader.
