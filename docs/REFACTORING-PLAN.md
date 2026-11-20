# VYRA Studio Refactoring Plan

## Goal
Keep the existing Android-first implementation working while making the project understandable and safe to extend.

## Phase 1 — UI file separation
- [x] Keep the Android entry point and tab navigation in MainActivity.
- [x] Split Overview, Store, Patcher, Creator, Sources, and Builds into focused screen files.
- [x] Move shared layout components into SharedComponents.
- [ ] Verify CI build and unit tests after the refactor.

## Phase 2 — Store experience
- [ ] Add searchable app catalog and category filters.
- [ ] Add app detail screens driven by repository metadata.
- [ ] Show availability truthfully; never expose a VYRA APK action without a verified release.
- [ ] Add loading, empty, offline, and retry states.

## Phase 3 — Release and update pipeline
- [ ] Define a per-app release manifest in VYRA-REPOSITORY.
- [ ] Verify APK size, SHA-256, package identity, and signing certificate before install handoff.
- [ ] Integrate Android's user-confirmed package installer flow.
- [ ] Generalize the YouTube-only update prototype only after the first app passes end-to-end testing.

## Phase 4 — Patcher boundaries
- [ ] Keep APK inspection read-only and isolated from patch execution.
- [ ] Define a patch-engine adapter contract.
- [ ] Integrate an engine only after runtime, ABI, and license review.
- [ ] Do not label a patch bundle as an installable APK.

## Safety rules
- Never delete existing working functionality as part of cleanup.
- Do not commit signing keys, passwords, or generated APKs.
- Do not claim an APK is verified until integrity, package, signature, install, and smoke checks pass.
