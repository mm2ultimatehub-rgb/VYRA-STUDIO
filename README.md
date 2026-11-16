# VYRA STUDIO

An independent Android workspace for patch discovery, repository management, APK inspection, patch creation, and build orchestration.

## Current milestone

The native Android shell is built with Kotlin and Jetpack Compose.

Implemented:
- Dark-first Material 3 application shell with Overview, Store, Patcher, Creator, Sources, and Builds screens.
- Android document picker for user-selected APK files.
- Background APK inspection: package name, app label, version, min/target SDK, APK SHA-256, and signer certificate SHA-256 fingerprints.
- Version 1 repository-index model and JSON parser with schema, package-name, version-range, HTTPS URL, SHA-256, and artifact-format validation.
- Patch compatibility matching by package name and version-code range, with unit tests.
- GitHub Actions workflow that runs unit tests and attempts a debug APK build on pushes and pull requests to `main`.

Not implemented:
- Remote repository fetching, refresh, and source-management UI.
- Compatibility display in the Store/Patcher screens.
- Patch-engine integration.
- APK modification, re-signing, or patched APK export.

APK inspection is read-only. It copies the selected file to app-private cache temporarily, reads package metadata, computes fingerprints, and deletes the temporary copy. It does not modify the selected APK.

The repository parser validates metadata only. It never executes patch code. HTTPS and a declared SHA-256 digest do not by themselves establish that a source is trustworthy.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.13

Run from the repository root:

```bash
gradle testDebugUnitTest assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow is defined at `.github/workflows/android-ci.yml`. A configured workflow is not proof of a successful build; check the repository's Actions tab for the latest run.

## Architecture and research

- [Product scope](docs/PRODUCT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Android-first decision](docs/decisions/0001-android-first.md)
- [Repository index format v1](docs/REPOSITORY-INDEX-V1.md)
- [Upstream patching research and license notes](docs/UPSTREAM-RESEARCH.md)
- [Roadmap](docs/ROADMAP.md)

## Safety and licensing

Only use APKs you are authorized to modify. VYRA does not distribute proprietary APKs. External patch sources must be treated as untrusted until reviewed and explicitly added by the user.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional Section 7 conditions. No upstream engine or patch implementation is included in this application shell. Any future reuse must preserve notices and comply with the applicable license terms.


## Current repository workflow
VYRA can add HTTPS v1 repository indexes, retain source URLs locally, list patch metadata, and compare patch targets with an inspected APK. A user can explicitly download an MPP artifact; the app verifies its SHA-256 digest before retaining it in its private cache. Downloads are size-limited and redirects are rejected.

**Not implemented yet:** applying patches, executing patch payloads, rebuilding/signing APKs, or exporting patched applications. A verified download is not a successful patch build.
