# VYRA STUDIO — Initial Architecture

This is a proposal, not a finalized framework decision.

## Candidate platform
Kotlin Multiplatform with Compose Multiplatform is the leading candidate for shared application logic and UI across Android and desktop.

## Proposed modules
- `core:model`: app, patch, repository, and build domain models.
- `core:compatibility`: version, dependency, and conflict checks.
- `core:repository`: repository index parsing and validation.
- `core:patching`: patch pipeline interfaces and orchestration.
- `core:security`: hashes, signature inspection, and secret handling.
- `feature:store`: patch discovery and details.
- `feature:patcher`: APK import, patch selection, and build flow.
- `feature:creator`: patch authoring and validation.
- `feature:repositories`: repository management.
- `feature:builds`: build history, logs, and exports.
- `app:android` and `app:desktop`: platform entry points.

## Design constraints
- Keep platform-specific APK operations behind interfaces.
- Keep UI independent from patch-engine implementation details.
- Make long-running builds observable and safely cancellable.
- Use deterministic, versioned patch manifests.
- Store secrets outside source control and never log them.

## Decisions to confirm
1. Kotlin Multiplatform + Compose Multiplatform or another shared UI stack.
2. Desktop targets: Windows first or Windows/macOS/Linux together.
3. Integrate a compatible existing open-source patch engine or build a new engine.

## License review
Before adapting upstream code, inspect its exact license and obligations. Keep VYRA branding and code provenance distinct from upstream projects.
