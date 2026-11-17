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


## Source audit — 2026-11-17

The upstream `MorpheApp/morphe-manager` default branch was inspected directly. The repository reports GPL-3.0 and contains two Gradle modules: `:app` and `:patcher-core`. The settings file also supports a local composite build for `morphe-patcher`, while the app normally resolves the published `app.morphe:morphe-patcher` artifact.

### Confirmed engine dependencies

- `:patcher-core` is a Kotlin/JVM module, deliberately separated from Android APIs. It owns the patch pipeline, request/session/listener abstractions, host command/event protocol, split APK preparation, and resource monitoring.
- The app depends on both `:patcher-core` and `app.morphe:morphe-patcher`, and packages a separately built/shrunk `patcher-core` host JAR for the isolated patcher process.
- The app exposes the patcher through an Android service and AIDL interfaces. The service starts a separate `:patcher` process; the app and host communicate through a pipe-based JSON command/event protocol.
- The upstream app embeds Android-compatible OpenJDK images: Java 25 for arm64-v8a and x86_64, Java 17 for armeabi-v7a. It verifies runtime archives with pinned SHA-256 values, prepares reduced runtime images, and packages native launcher support. ART is used as a fallback and on unsupported ABIs.
- The app build includes native CMake configuration, XZ runtime unpacking, Android document-file support, background work, dependency injection, networking, and signing/export flows. These are not supplied by `:patcher-core` alone.
- The inspected version catalog currently pins `app.morphe:morphe-patcher:2.4.0` and a Morphe-maintained ARSCLib revision. These versions must be pinned deliberately; do not silently follow floating upstream versions.

### Integration decision

Do not transplant the patcher-core directory into the existing shell as an isolated first step and do not attempt to invoke MPP DEX code from the current inspector. The minimum credible functional baseline is the upstream app + patcher-core + patcher artifact + service/AIDL + embedded runtime/native launcher + APK split/signing flow, built and tested together.

The safest next implementation milestone is a controlled upstream baseline import into a dedicated integration branch or a clean VYRA-derived working tree, preserving upstream history/notices and then applying VYRA branding as a small, reviewable patch. Keep the current VYRA shell as a reference until the upstream baseline builds; do not merge the two architectures prematurely.

### Compatibility and build risks

- The upstream source currently declares a newer Android Gradle Plugin/toolchain and compile SDK than the current VYRA shell. Align the Gradle wrapper, JDK, SDK, Kotlin, Compose compiler, native CMake/NDK, and CI image as a single tested set.
- The embedded JVM assets make CI builds larger and slower and require network downloads. Keep the upstream SHA-256 checks intact and cache only verified artifacts.
- The manager's patcher artifact and ARSCLib are separate upstream dependencies. Verify repository availability, release provenance, and transitive licenses before relying on them.
- Device validation must cover arm64-v8a first, then armeabi-v7a and x86_64; test ART fallback, split APK merge/preparation, memory limits, cancellation, signing, and install handoff.
- The current VYRA debug CI success proves only that the current shell builds. It does not prove the upstream patching engine has been integrated or that a VYRA APK can patch an application.

