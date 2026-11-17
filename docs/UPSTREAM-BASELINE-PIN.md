# VYRA upstream baseline pin

This branch is reserved for the controlled Morphe Manager source adoption.

## Pinned upstream

- Repository: https://github.com/MorpheApp/morphe-manager
- Upstream branch inspected: `main`
- Exact inspected commit: `d9f56746d13890c0f8837c6d070dae6f210ad2a9`
- Commit date: 2026-11-15
- Commit subject: `chore: Publish release v2.6.0 [skip ci]`
- License reported by GitHub: GPL-3.0

The pin is intentional: integration work must use this exact source snapshot until a later update is explicitly reviewed and pinned. Never build VYRA from a moving `main` branch.

## Integration scope

The baseline import must include, as a coordinated unit:

1. Upstream Android app module and its resources/assets.
2. `:patcher-core` and its host-JAR/shrinking tasks.
3. The exact compatible `app.morphe:morphe-patcher` artifact and ARSCLib revision declared by this source snapshot.
4. Android service, AIDL interfaces, process isolation and pipe protocol.
5. Embedded JVM preparation, verified runtime archives, native launcher/CMake sources and ABI packaging.
6. Split APK preparation, signing/export and installer handoff.
7. Tests, Gradle build logic, dependency/license metadata, LICENSE, NOTICE and all required attribution.

Do not cherry-pick only `:patcher-core`: it is not a standalone Android patching application.

## Branch safety

- This branch starts from VYRA `main`; it does not replace or rewrite `main`.
- First milestone: import the pinned upstream source with its provenance and notices intact.
- Second milestone: reproduce a clean upstream-equivalent build before any VYRA branding changes.
- Third milestone: apply VYRA identity as a small, reviewable patch while retaining the functional engine.
- Keep the existing VYRA shell available until the upstream-derived baseline builds and passes tests.

## Build acceptance gates

A successful Gradle assemble alone is not enough. Record:
- exact upstream and VYRA commit IDs;
- JDK, Gradle, AGP, Kotlin, Android SDK, NDK and CMake versions;
- resolved patcher and ARSCLib versions;
- CI build and unit-test results;
- APK output SHA-256 and signing certificate fingerprint;
- supported ABI/device test results.

No claim of real patching support is permitted until an actual compatible patch bundle has been applied to a test APK, the resulting APK has been signed and validated, and the output installs and launches on a test device.

## Legal and distribution gate

This is a GPL-3.0-derived codebase. Before distributing builds, preserve notices and source history as required, mark VYRA modifications and dates, publish the corresponding source under the applicable license terms, display appropriate legal notices, and audit bundled dependencies/assets separately. This checklist is not legal advice.
