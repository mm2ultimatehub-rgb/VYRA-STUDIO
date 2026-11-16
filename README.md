# VYRA STUDIO

An independent Android workspace for patch discovery, repository management, APK inspection, patch creation, and build orchestration.

## Current milestone

The native Android shell is built with Kotlin and Jetpack Compose.

Implemented:
- Dark-first Material 3 application shell.
- Overview, Store, Patcher, Creator, Sources, and Builds screens.
- Android document picker for user-selected APK files.
- Background APK inspection: package name, app label, version, min/target SDK, APK SHA-256, and signer certificate SHA-256 fingerprints.
- GitHub Actions workflow that attempts a debug APK build on pushes and pull requests to `main`.

Not implemented:
- Patch catalog/repository network refresh.
- Compatibility matching against patch metadata.
- Patch-engine integration.
- APK modification, re-signing, or patched APK export.

APK inspection copies the selected file to the app's private cache temporarily, reads package metadata, computes fingerprints, and deletes the temporary copy. It does not modify the selected APK.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.13

Run from the repository root:

```bash
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow is defined at `.github/workflows/android-ci.yml`. A configured workflow is not proof of a successful build; check the repository's Actions tab for the latest run.

## Architecture and research

- [Product scope](docs/PRODUCT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Android-first decision](docs/decisions/0001-android-first.md)
- [Upstream patching research and license notes](docs/UPSTREAM-RESEARCH.md)
- [Roadmap](docs/ROADMAP.md)

## Safety and licensing

Only use APKs you are authorized to modify. VYRA does not distribute proprietary APKs. External patch sources must be treated as executable code and reviewed before use.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional Section 7 conditions. No upstream engine or patch implementation is included in this application shell. Any future reuse must preserve notices and comply with the applicable license terms.
