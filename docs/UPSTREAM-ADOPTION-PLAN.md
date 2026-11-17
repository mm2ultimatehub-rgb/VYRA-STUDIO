# VYRA STUDIO — Morphe Manager adoption plan

## Product direction

VYRA STUDIO is not intended to be a new, unrelated APK inspector. The product direction is to use the upstream Morphe Manager implementation as the functional baseline, then ship a clearly distinct VYRA-branded derivative.

The target is functional parity with the upstream patching workflow, not a visual clone:
- import APKs and split APK sets;
- discover and select compatible patches from patch bundles;
- configure patch options and dependencies;
- run the patch pipeline with progress, logs, cancellation, and resource monitoring;
- rebuild, sign, validate, and export the resulting APK;
- retain repository/source management and patch bundle compatibility.

VYRA's visual identity is its own: VYRA STUDIO name, a lilac/purple accent palette, and VYRA icons, typography, and screen branding. Do not use Morphe names, logos, or marks as VYRA branding.

## Recommended implementation strategy

Prefer a source-based upstream adoption/fork over reimplementing the engine or treating the patcher as a simple Maven dependency.

The current Morphe Manager source tree contains several tightly connected parts:
- the Android app and patcher UI/view models;
- patcher-core for patch orchestration, host protocol, split APK preparation, and resource monitoring;
- a patcher service and IPC/AIDL boundary;
- embedded JVM runtime installation/launch support and native launcher code;
- patch bundle metadata and patch-selection logic;
- signing, validation, and export flows.

These pieces form one runtime system. Copying only patcher-core or adding a library dependency is not sufficient to reproduce the Manager's Android behavior. The upstream implementation must be inspected as a whole and brought forward with its build scripts, assets, native code, runtime packaging, tests, and required notices.

## Migration sequence

1. Freeze feature work on the current standalone shell; keep its useful APK inspection, repository validation, digest verification, and tests as reference material.
2. Pin an upstream Morphe Manager commit and record its exact origin and date.
3. Review the complete upstream LICENSE, NOTICE, dependency licenses, bundled assets, and build/runtime requirements before importing files.
4. Establish a reproducible VYRA fork baseline with upstream source history/attribution preserved as required.
5. Apply only the VYRA identity layer first: application label, package/application identifiers where safe, lilac theme, VYRA icon, and product copy. Keep functional patching behavior unchanged during this step.
6. Build and test the unmodified baseline before making engine changes.
7. Add VYRA-specific repository/store experiences only after the upstream patch workflow works end to end.
8. Validate on real Android devices and supported ABIs, including split APK inputs, low-memory behavior, cancellation, signing, and output installation.

## Licensing and attribution gate

The upstream Morphe Manager repository identifies itself as GPL-3.0 and publishes additional Section 7 conditions in its NOTICE. Its README says derivative works must use distinct branding and must not use Morphe's name, logos, or trademarks as their branding/title.

Before distributing a derivative:
- preserve applicable copyright, license, warranty-disclaimer, and attribution notices;
- identify VYRA as a modified derivative and document the modifications and date;
- provide the corresponding source and comply with GPL-3.0 obligations for the covered work;
- include and surface the applicable legal notices in the app;
- do not imply endorsement by or affiliation with Morphe;
- audit every bundled dependency, runtime, native library, and asset separately.

This is an engineering compliance checklist, not legal advice. If VYRA will be distributed publicly or commercially, obtain a qualified license review.

## Current state

The existing VYRA repository is a small Kotlin/Compose shell with APK inspection, declarative repository metadata, MPP download/hash verification, and non-executing MPP structure inspection. It does not currently apply patches, host the upstream patcher runtime, rebuild/sign APKs, or export patched applications.

The previous shell should not be described as a working Morphe-like patcher. The next major milestone is to establish and verify the upstream-derived functional baseline.
