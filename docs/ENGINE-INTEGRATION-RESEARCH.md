# VYRA STUDIO — Patch engine integration research

Research date: 2026-11-17

## Executive finding

Morphe Patcher is a reusable Kotlin/JVM library, but embedding it in an Android app is not a one-line dependency change. Morphe Manager uses a dedicated patcher host, a separate host JAR, a foreground service/process boundary, and embedded HotSpot JVM images for supported ABIs. It can also run the host on Android ART. VYRA should not add the engine dependency until its runtime, bundle loader, signing, memory, and GPL distribution plan are deliberately implemented.

## Verified upstream implementation facts

### Patcher library

The current Morphe Patcher build publishes group `app.morphe`, artifact `morphe-patcher`. Its current source build uses Kotlin 2.4.21 and targets JVM 11 bytecode. It depends on Android build tools libraries, apkzlib, apksig, ARSCLib, Guava, Bouncy Castle, Kotlin reflection/coroutines/serialization, and Smali.

The Patcher README describes DEX bytecode, Android resource tables, and arbitrary APK file modification. Patch modules can declare dependencies.

Sources:
- https://github.com/MorpheApp/morphe-patcher
- https://github.com/MorpheApp/morphe-patcher/blob/main/build.gradle.kts
- https://github.com/MorpheApp/morphe-patcher/blob/main/gradle/libs.versions.toml
- https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_patches_intro.md

### Android runtime is a substantial subsystem

Morphe Manager does more than add `implementation("app.morphe:morphe-patcher:2.4.0")`:
- It has a separate `:patcher-core` module, whose host JAR bundles the patcher and runtime dependencies.
- It runs a patching host in a dedicated process/service so memory exhaustion or a failed run does not take down the main UI process.
- For JVM mode it embeds HotSpot runtime images by ABI, extracts/prepares them, starts the JVM through JNI, and manages heap limits.
- It also supports a host path on Android ART.
- It handles split APK preparation, progress reporting, resource monitoring, cancellation/process lifecycle, and output signing.

Sources:
- https://github.com/MorpheApp/morphe-manager/blob/main/app/build.gradle.kts
- https://github.com/MorpheApp/morphe-manager/blob/main/app/src/main/java/app/morphe/manager/patcher/runtime/PatcherService.kt
- https://github.com/MorpheApp/morphe-manager/blob/main/app/src/main/java/app/morphe/manager/patcher/runtime/jvm/EmbeddedJvm.kt
- https://github.com/MorpheApp/morphe-manager/blob/main/patcher-core/build.gradle.kts
- https://github.com/MorpheApp/morphe-manager/blob/main/patcher-core/src/main/kotlin/app/morphe/manager/patcher/PatchPipeline.kt

### MPP bundle structure

Morphe Manager treats an MPP bundle as a JAR file. It reads the JAR manifest attributes including `Name`, `Version`, `Description`, `Source`, `Author`, `Contact`, `Website`, `License`, and `Patcher-Version`. Before loading patches, it checks that the bundle has non-empty DEX entries. Patch classes are loaded from DEX on ART; JVM mode loads patch classes from the host's class files.

This is a critical distinction from VYRA's repository-index JSON. The JSON index is catalog metadata; the MPP JAR is a code-bearing patch bundle. VYRA must never treat parsing a manifest as equivalent to validating or safely executing the bundle.

Source:
- https://github.com/MorpheApp/morphe-manager/blob/main/app/src/main/java/app/morphe/manager/patcher/patch/PatchBundle.kt
- https://github.com/MorpheApp/morphe-manager/blob/main/patcher-core/src/main/kotlin/app/morphe/manager/patcher/PatchPipeline.kt

## Recommended VYRA implementation sequence

1. Keep the current patcher UI and engine behind a stable `PatchingEngine` interface.
2. Implement a non-executing MPP inspection stage: ZIP/JAR validity, manifest metadata, non-empty DEX entries, size/count limits, and SHA-256. Never load patch classes during inspection.
3. Add dependency/conflict metadata to VYRA's own catalog schema without pretending the metadata can inspect arbitrary MPP code.
4. Prototype engine execution in an isolated worker process with strict memory, storage, timeout, and cancellation handling. A normal coroutine on the UI process is not sufficient for large APKs.
5. Decide whether to implement an ART host, embed a HotSpot JVM per supported ABI, or use a separate desktop/remote build worker. Each has major trade-offs.
6. Complete GPL-3.0 and Section 7 compliance review before shipping any derivative integration. Preserve notices, publish corresponding source where required, and use distinct VYRA branding. This document is technical research, not legal advice.
7. Test first with a user-authorized APK and a reversible patch. Keep the original file unchanged and verify the output APK signature and installability.

## Current VYRA boundary

VYRA currently imports and inspects APK metadata, stores repository URLs, validates declarative repository indexes, matches package/version constraints, downloads MPP artifacts on explicit user action, and verifies their SHA-256 digest.

VYRA does not yet parse MPP internals, load or execute patch code, rewrite DEX/resources, rebuild APKs, sign patched APKs, or install/export patched outputs.
