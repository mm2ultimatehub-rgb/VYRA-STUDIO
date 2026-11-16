# VYRA STUDIO — Roadmap

## Phase 0 — Product foundation
- [x] Confirm repository and default branch.
- [x] Record initial product scope.
- [x] Research Morphe patch formats, engine capabilities, and license conditions.
- [x] Decide Android-first native architecture.
- [ ] Complete detailed upstream dependency and runtime compatibility review (GPL obligations reviewed at a high level; runtime integration decision and device validation remain open).

## Phase 1 — Android application shell
- [x] Commit native Android project skeleton and navigation shell.
- [x] Establish dark-first Material 3 theme.
- [x] Add Store, Patcher, Creator, Repositories, and Builds destinations.
- [x] Add APK document picker.
- [x] Add APK package metadata and SHA-256 signer/file fingerprint inspection.
- [x] Add GitHub Actions workflow for unit tests and debug APK packaging.
- [x] Verify unit tests and clean debug APK packaging in GitHub Actions (run #58, 2026-11-16).
- [ ] Add instrumentation tests.

## Phase 2 — Repository and patch catalog
- [x] Define repository index format v1.
- [x] Implement JSON parser and schema validation.
- [x] Validate package names, version bounds, HTTPS artifact URLs, SHA-256 values, and supported bundle format.
- [x] Implement package/version-code compatibility matching.
- [x] Implement remote repository refresh and validation.
- [x] Add source trust prompts and provenance display.
- [x] Show patch metadata and compatibility in the Store/Patcher screens.

## Phase 3 — APK inspection and patch pipeline
- [x] Import APKs using Android's Storage Access Framework.
- [x] Inspect package metadata and signing-certificate fingerprints.
- [ ] Add dependency and conflict checks.
- [ ] Select and integrate a patch engine after license/runtime review.
- [x] Inspect downloaded MPP JAR manifests and DEX entry structure without loading or executing patch code.
- [ ] Add build logs, validation, and artifact export.

## Phase 4 — Creator and release quality
- [ ] Add patch authoring and manifest validation.
- [ ] Add package integrity checks and signing guidance.
- [ ] Add release automation.

## Later
- [ ] Evaluate a desktop companion after Android workflow validation.
