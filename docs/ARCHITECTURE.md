# VYRA STUDIO — Architecture

## Current decision

The MVP is **Android-first and native**: Kotlin, Jetpack Compose, Material 3, and Android Gradle Plugin. See [ADR 0001](decisions/0001-android-first.md) for the rationale.

Desktop is deferred until the Android workflow is validated. Shared domain contracts can be extracted later if a desktop client becomes worthwhile.

## Modules and boundaries

- `app`: Android entry point, navigation, lifecycle, and dependency wiring.
- `core:model`: app, patch, repository, and build domain models.
- `core:compatibility`: target-app version, dependency, and conflict checks.
- `core:repository`: repository index parsing, refresh, and validation.
- `core:apk`: APK metadata and signing-certificate inspection.
- `core:patching`: patch-engine interface, orchestration, progress, and cancellation.
- `feature:store`: patch discovery and details.
- `feature:patcher`: APK import, patch selection, and build flow.
- `feature:creator`: patch authoring and validation.
- `feature:repositories`: repository management.
- `feature:builds`: build history, logs, validation, and exports.

The first code milestone may keep these packages inside a single Android module. Split Gradle modules only when dependencies and build times justify it.

## Patch engine

The UI must not depend on a specific patch engine. Keep an adapter boundary in `core:patching`.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional GPLv3 Section 7 conditions. No upstream implementation is integrated yet. See [upstream research](UPSTREAM-RESEARCH.md) and the upstream LICENSE/NOTICE files before reuse.

## Design and runtime constraints

- Use a premium, uncluttered dark-first interface with accessible contrast.
- Keep platform-specific APK operations behind clear interfaces.
- Keep UI independent from patch-engine implementation details.
- Run long operations away from the main thread; expose progress and safe cancellation.
- Use deterministic, versioned patch manifests.
- Treat patch sources as executable code: show provenance and require explicit trust decisions.
- Store secrets outside source control and never log them.
- Never distribute proprietary APKs or signing material.

## Deferred decisions

1. Exact patch-engine integration after runtime and license validation.
2. Desktop client only after the Android MVP proves the workflow.
3. Remote build infrastructure only if on-device processing is not viable.
