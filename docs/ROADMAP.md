# VYRA STUDIO — Roadmap

## Phase 0 — Product foundation
- [x] Confirm repository and default branch.
- [x] Record initial product scope.
- [x] Research Morphe patch formats, engine capabilities, and license conditions.
- [x] Decide Android-first native architecture.
- [ ] Complete detailed upstream dependency and runtime compatibility review.

## Phase 1 — Android application shell
- [x] Commit Android project skeleton and first navigation shell.
- [x] Establish dark-first design tokens and Material 3 theme.
- [x] Add Store, Patcher, Creator, Repositories, and Builds navigation destinations.
- [ ] Add Gradle wrapper and verify a clean Android build in CI.
- [ ] Add UI and domain unit tests.

## Phase 2 — Repository and patch catalog
- [ ] Define a versioned repository index format.
- [ ] Implement repository refresh and validation.
- [ ] Implement patch metadata, detail pages, and compatibility display.
- [ ] Add source trust prompts and provenance display.

## Phase 3 — APK inspection and patch pipeline
- [ ] Import APKs using Android's Storage Access Framework.
- [ ] Inspect package metadata and signing certificates.
- [ ] Implement compatibility and dependency checks.
- [ ] Select and integrate a patch engine after license/runtime review.
- [ ] Add build logs, validation, and artifact export.

## Phase 4 — Creator and release quality
- [ ] Add patch authoring and manifest validation.
- [ ] Add package integrity checks and signing guidance.
- [ ] Add tests, documentation, and release automation.

## Later
- [ ] Evaluate a desktop companion after Android workflow validation.
