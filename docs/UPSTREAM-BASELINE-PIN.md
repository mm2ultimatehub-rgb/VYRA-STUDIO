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


## Pinned source checkout

The upstream repository is also recorded as a Git submodule at `upstream/morphe-manager`. Its gitlink is pinned to the exact commit above, not to a branch tip. A full local checkout therefore uses:

```bash
git clone --branch integration/morphe-baseline --recurse-submodules https://github.com/mm2ultimatehub-rgb/VYRA-STUDIO.git
```

The submodule is a provenance/source-staging step only. The current VYRA Android build does not yet consume or compile it. Do not describe the patch engine as integrated until the Gradle project, Android service/runtime, patch bundle execution, signing and device validation are connected and tested.


## Pinned patcher dependency graph

The patcher library is a separate source project and is also pinned as a submodule. Its source-based composite build is important because the Manager's published Maven package is hosted in Morphe's GitHub Packages registry; the upstream CI explicitly supplies a GitHub token to resolve it. VYRA must not assume that its own repository token can read another owner's package.

| Source | Pinned commit | License reported by GitHub |
|---|---|---|
| `MorpheApp/morphe-manager` | `d9f56746d13890c0f8837c6d070dae6f210ad2a9` | GPL-3.0 |
| `MorpheApp/morphe-patcher` | `0aed87c4e3264cf9233feab7ed26cf2543a08b96` | GPL-3.0 |
| `MorpheApp/ARSCLib` | `50418a07a806f7fc6d8a567be81d951d6e360079` | Apache-2.0 |
| `MorpheApp/smali` | `bbabf5f9980028daba64c19366483bbddc05cf01` | GPL-3.0 for MorpheApp changes; audit upstream portions separately |

All four sources are pinned under `upstream/` as submodules. The upstream project files expect sibling source directories for local composite builds. The intended checkout layout therefore deliberately places `morphe-manager`, `morphe-patcher`, `ARSCLib` and `smali` side by side under `upstream/`.

The patcher source build itself still needs its exact transitive dependencies and toolchain validated. In particular, review the smali license, patcher version catalog, ARSCLib substitution and any additional JitPack/GitHub package requirements before enabling the composite build.


## Baseline verification automation

- `scripts/verify-upstream-baseline.sh` initializes all four submodules, checks each exact commit, then runs the upstream Manager's debug unit tests and debug APK build.
- `.github/workflows/verify-upstream-baseline.yml` runs that script on pushes to this integration branch and can also be dispatched manually.
- The workflow uploads the upstream debug APK only if tests and assembly succeed.

This is a source-baseline verification job, not yet the VYRA-branded application build. Its first run must be inspected for missing SDK/NDK components, dependency repository permissions, and toolchain issues. The upstream manager's own CI explicitly passes a GitHub token to resolve Morphe's package registry; the VYRA repository token may not have cross-owner package access. The source composite builds are pinned to avoid the primary patcher artifact dependency, but other private package dependencies may still require a permitted source or credential path.
