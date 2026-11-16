# VYRA STUDIO

An independent Android workspace for patch discovery, repository management, APK inspection, patch creation, and build orchestration.

## Project status

The repository now contains the first native Android application shell built with Kotlin and Jetpack Compose.

Implemented in this milestone:
- Dark-first Material 3 interface.
- Overview, Store, Patcher, Sources, and Builds destinations.
- Patch Creator entry from the overview.
- Android document picker for selecting a user-provided APK.

Not implemented yet:
- APK metadata/signature inspection.
- Patch repository network refresh.
- Patch-engine integration.
- Actual APK modification, signing, or export.

The current APK picker is only an import UI. It does not patch or alter the selected file.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 8.13

From the repository root:

```bash
gradle assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs the same debug build on pushes and pull requests to `main`, then publishes the APK as a workflow artifact when the build succeeds.

## Architecture and research

- [Product scope](docs/PRODUCT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Android-first decision](docs/decisions/0001-android-first.md)
- [Upstream patching research and license notes](docs/UPSTREAM-RESEARCH.md)
- [Roadmap](docs/ROADMAP.md)

## Safety and licensing

Only use APKs you are authorized to modify. VYRA does not distribute proprietary APKs. External patch sources must be treated as executable code and reviewed before use.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional Section 7 conditions. No upstream engine or patch implementation is included in this initial application shell. Any future reuse must preserve notices and comply with the applicable license terms.
