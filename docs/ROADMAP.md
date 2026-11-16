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
- [x] Add GitHub Actions workflow for unit tests and debug APK packaging.
- [ ] Add Gradle wrapper and verify a clean Android build in CI.
- [ ] Add instrumentation tests.

## Phase 2 — Repository and patch catalog
- [x] Define repository index format v1.
- [x] Implement JSON parser and schema validation.
- [x] Validate package names, version bounds, HTTPS artifact URLs, SHA-256 values, and supported bundle format.
- [x] Implement package/version-code compatibility matching.
- [ ] Implement remote repository refresh and validation.
- [ ] Add source trust prompts and provenance display.
- [ ] Show patch metadata and compatibility in the Store/Patcher screens.

## Phase 3 — APK inspection and patch pipeline
- [x] Import APKs using Android's Storage Access Framework.
- [x] Inspect package metadata and signing-certificate fingerprints.
- [ ] Add dependency and conflict checks.
- [ ] Select and integrate a patch engine after license/runtime review.
- [ ] Add build logs, validation, and artifact export.

## Phase 4 — Creator and release quality
- [ ] Add patch authoring and manifest validation.
- [ ] Add package integrity checks and signing guidance.
- [ ] Add release automation.

## Later
- [ ] Evaluate a desktop companion after Android workflow validation.
