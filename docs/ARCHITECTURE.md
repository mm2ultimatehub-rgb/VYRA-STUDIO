# VYRA STUDIO — Architecture

## Current decision

The MVP is Android-first and native: Kotlin, Jetpack Compose, Material 3, and Android Gradle Plugin. See [ADR 0001](decisions/0001-android-first.md).

## Main boundaries

- `app`: Android entry point, navigation, lifecycle, dependency wiring.
- `core:model`: app, patch, repository, and build domain models.
- `core:compatibility`: target-app version, dependency, and conflict checks.
- `core:repository`: repository index parsing, refresh, and validation.
- `core:apk`: APK metadata and signing-certificate inspection.
- `core:patching`: patch-engine interface, orchestration, progress, cancellation.
- `feature:store`: patch discovery and details.
- `feature:patcher`: APK import, patch selection, and build flow.
- `feature:creator`: patch authoring and validation.
- `feature:repositories`: repository management.
- `feature:builds`: build history, logs, validation, and exports.

The current MVP keeps UI code in a small Android module while the APK inspector is isolated under the `apk` package. Further Gradle module splitting can wait until the domain stabilizes.

## APK inspection

The Patcher uses Android's Storage Access Framework to receive a user-selected APK URI. The inspector copies it to app-private cache on an IO dispatcher, parses package metadata with PackageManager, and computes SHA-256 for the APK and available signing certificates. The temporary copy is deleted after inspection.

Inspection is read-only. It does not validate patch compatibility, modify APK contents, or prove that an APK is safe.

## Patch engine

The UI must not depend on a specific patch engine. Keep an adapter boundary in the patching layer.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional GPLv3 Section 7 conditions. No upstream implementation is integrated. See [upstream research](UPSTREAM-RESEARCH.md) and upstream LICENSE/NOTICE files before reuse.

## Runtime and security constraints

- Use a premium, uncluttered dark-first interface with accessible contrast.
- Run long operations away from the main thread and expose progress.
- Treat patch sources as executable code; show provenance and require explicit trust decisions.
- Store secrets outside source control and never log them.
- Never distribute proprietary APKs or signing material.
- Do not claim a patch is compatible until package/version/dependency checks have run.

## Deferred decisions

1. Exact patch-engine integration after runtime and license validation.
2. Desktop client after the Android MVP validates the workflow.
3. Remote build infrastructure only if on-device processing is not viable.
