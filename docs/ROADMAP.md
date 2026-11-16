# VYRA STUDIO — Roadmap

## Phase 0 — Product foundation
- [x] Confirm repository and default branch.
- [x] Record initial product scope.
- [x] Research Morphe patch formats, engine capabilities, and license conditions.
- [x] Decide Android-first native architecture.
- [ ] Complete detailed upstream dependency and runtime compatibility review.

## Phase 1 — Android application shell
- [x] Commit native Android project skeleton and navigation shell.
- [x] Establish dark-first Material 3 theme.
- [x] Add Store, Patcher, Creator, Repositories, and Builds destinations.
- [x] Add APK document picker.
- [x] Add APK package metadata and SHA-256 signer/file fingerprint inspection.
- [x] Add GitHub Actions debug build workflow.
- [ ] Add Gradle wrapper and verify a clean Android build in CI.
- [ ] Add automated unit and instrumentation tests.

## Phase 2 — Repository and patch catalog
- [ ] Define a versioned repository index format.
- [ ] Implement repository refresh and validation.
- [ ] Implement patch metadata, detail pages, and compatibility display.
- [ ] Add source trust prompts and provenance display.

## Phase 3 — APK inspection and patch pipeline
- [x] Import APKs using Android's Storage Access Framework.
- [x] Inspect package metadata and signing-certificate fingerprints.
- [ ] Implement compatibility and dependency checks.
- [ ] Select and integrate a patch engine after license/runtime review.
- [ ] Add build logs, validation, and artifact export.

## Phase 4 — Creator and release quality
- [ ] Add patch authoring and manifest validation.
- [ ] Add package integrity checks and signing guidance.
- [ ] Add tests, documentation, and release automation.

## Later
- [ ] Evaluate a desktop companion after Android workflow validation.
