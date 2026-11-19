# VYRA STUDIO

VYRA STUDIO is an Android APK patching project intended to use the Morphe Manager patching workflow as its functional baseline, with a distinct VYRA identity and lilac visual theme.

## Product direction

The goal is not to build an unrelated APK inspector. VYRA aims to provide a complete patching workflow: APK and split-APK import, compatible patch discovery, patch options, patch execution, progress/logging/cancellation, APK rebuild, signing, validation, and export.

The planned implementation strategy is documented in [Morphe Manager adoption plan](docs/UPSTREAM-ADOPTION-PLAN.md).

## Current implementation status

The current repository is an early Kotlin/Jetpack Compose shell. It currently includes:
- APK selection and read-only package/signing-certificate inspection;
- repository index parsing and validation;
- repository source management and HTTPS metadata fetching;
- patch artifact download with SHA-256 verification;
- non-executing MPP archive/manifest/DEX structure inspection;
- unit tests and a GitHub Actions debug build workflow.

**Important:** the current shell does not apply patches, run the Morphe patcher runtime, rebuild or sign patched APKs, or export patched applications. A successful debug build only verifies that this shell compiles and its unit tests pass; it is not a working patcher release.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36
- Gradle 8.14.4

Run from the repository root:

```bash
gradle testDebugUnitTest assembleDebug
```

The GitHub Actions workflow is defined at .github/workflows/android-ci.yml. See the repository Actions tab for verified build results and artifacts.

## Research and architecture

- [Morphe Manager adoption plan](docs/UPSTREAM-ADOPTION-PLAN.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Engine integration research](docs/ENGINE-INTEGRATION-RESEARCH.md)
- [Verified patcher runtime adoption contract](docs/PATCHER-RUNTIME-ADOPTION.md)
- [Upstream research and license notes](docs/UPSTREAM-RESEARCH.md)
- [Roadmap](docs/ROADMAP.md)

## Branding, licensing, and safety

VYRA will use its own name, lilac palette, icons, and product identity. It must not use Morphe's name, logos, or marks as VYRA branding.

Morphe Manager is published under GPL-3.0 with additional Section 7 conditions described in its upstream NOTICE. Any derivative implementation must preserve applicable notices, identify modifications, provide corresponding source, and satisfy the license conditions before distribution. Review all bundled dependencies and assets separately. This repository's adoption plan is an engineering checklist, not legal advice.

Only modify APKs you are authorized to modify. Do not distribute proprietary APKs or signing keys. Treat patch bundles as executable code: inspect provenance, verify integrity, and only run trusted bundles in the patcher runtime.
