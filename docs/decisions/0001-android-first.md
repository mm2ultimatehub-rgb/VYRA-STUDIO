# ADR 0001: Android-first application architecture

- Status: Accepted for the MVP
- Date: 2026-11-17

## Context

VYRA STUDIO's first useful release must run on Android and provide a clear path toward APK inspection, patch-source management, patch selection, and build orchestration. The repository currently contains product and architecture documentation only; no application framework or build system has been committed.

The patching workflow is Android-specific and requires platform APIs for document selection, package inspection, storage, and installation handoff. A shared desktop UI is not required to validate the first release.

## Decision

Build the first application as a native Android app using Kotlin, Jetpack Compose, Material 3, and the Android Gradle Plugin.

Keep the code modular:
- app: navigation, app lifecycle, dependency wiring
- core:model: typed domain models
- core:repository: patch-source and index contracts
- core:compatibility: app/version matching
- core:apk: APK metadata and signature inspection
- core:patching: engine abstraction and build orchestration
- feature:store, feature:patcher, feature:creator, feature:repositories, feature:builds: user-facing flows

Do not bind UI screens directly to a patch-engine implementation. Run long operations away from the main thread, expose progress, support cancellation where safe, and keep build logs free of secrets.

## Patch-engine strategy

Do not copy upstream patch implementations into the app during the UI-shell milestone. First evaluate the engine's Android runtime support, API stability, transitive dependencies, license, notices, and distribution obligations. An engine adapter will be implemented only after that review.

Morphe Patcher and Morphe Patches are GPL-3.0 projects with additional Section 7 conditions described by their upstream NOTICE files. Any reuse must preserve required notices and comply with GPL obligations; derivative branding must remain distinct. This ADR does not assert that the engine has already been integrated.

## Desktop direction

Desktop is deferred until the Android MVP validates the product and patch workflow. Shared domain contracts may be extracted later if a desktop client becomes worthwhile. Do not introduce Kotlin Multiplatform solely for speculative future reuse.

## Consequences

- The first build target is Android only.
- Android-native APIs can be used without platform abstraction overhead.
- Desktop support is explicitly deferred, not abandoned.
- The patch engine remains replaceable behind a stable interface.
