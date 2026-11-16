# VYRA STUDIO — Architecture

## Current decision

The MVP is Android-first and native: Kotlin, Jetpack Compose, Material 3, and Android Gradle Plugin. See [ADR 0001](decisions/0001-android-first.md).

## Main boundaries

- `app`: Android entry point, navigation, lifecycle, dependency wiring.
- `apk`: APK metadata and signing-certificate inspection.
- `repository`: repository-index models, JSON validation, and compatibility matching.
- `patching`: future patch-engine adapter, orchestration, progress, and cancellation.
- `feature:store`: patch discovery and details.
- `feature:patcher`: APK import, patch selection, and build flow.
- `feature:creator`: patch authoring and validation.
- `feature:repositories`: repository management.
- `feature:builds`: build history, logs, validation, and exports.

The current MVP keeps implementation in a small Android module while domain code is separated by package. Further Gradle module splitting can wait until the domain stabilizes.

## APK inspection

The Patcher uses Android's Storage Access Framework to receive a user-selected APK URI. The inspector copies it to app-private cache on an IO dispatcher, parses package metadata with PackageManager, and computes SHA-256 for the APK and available signing certificates. The temporary copy is deleted after inspection.

Inspection is read-only. It does not validate patch compatibility, modify APK contents, or prove that an APK is safe.

## Repository index v1

Repository indexes are declarative JSON. The parser validates schema version, IDs, target package names, version-code bounds, HTTPS artifact URLs, SHA-256 digests, and the supported `.mpp` artifact format. It never executes patch code.

Compatibility matching currently checks target package name and optional minimum/maximum version codes. Remote fetching, user trust prompts, artifact downloading, and digest verification at download time are still pending.

See [Repository index format v1](REPOSITORY-INDEX-V1.md).

## Patch engine

The UI must not depend on a specific patch engine. Keep an adapter boundary in the patching layer.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional GPLv3 Section 7 conditions. No upstream implementation is integrated. See [upstream research](UPSTREAM-RESEARCH.md) and upstream LICENSE/NOTICE files before reuse.

## Runtime and security constraints

- Use a premium, uncluttered dark-first interface with accessible contrast.
- Run long operations away from the main thread and expose progress.
- Treat patch sources as untrusted executable code; show provenance and require explicit trust decisions.
- Verify downloaded artifacts against declared digests before offering them to any patch engine.
- Store secrets outside source control and never log them.
- Never distribute proprietary APKs or signing material.
- Do not claim a patch is compatible until package/version/dependency checks have run.

## Deferred decisions

1. Exact patch-engine integration after runtime and license validation.
2. Desktop client after the Android MVP validates the workflow.
3. Remote build infrastructure only if on-device processing is not viable.


## Repository networking
- Repository index requests require HTTPS and reject credentials, URL fragments, and redirects.
- Connections use bounded connect/read timeouts and a 1 MB response limit.
- Repository metadata is parsed with the v1 schema validator; patch artifacts are not fetched or executed during source refresh.
- User-added source URLs are stored in app-private preferences (up to 30 sources).
- APK compatibility is matched locally using package name and optional version-code bounds.
- Patch artifacts are downloaded only after an explicit user action, over HTTPS, with redirects disabled and a 50 MB cap.
- The downloaded artifact is SHA-256 checked against repository metadata before it is kept in the app-private cache; failed integrity checks discard the temporary file.
- Verified MPP files are not executed or applied yet; patch-engine integration remains a separate milestone.
