# Morphe Engine Integration Plan

Status: architecture reviewed; engine is NOT integrated or executable in VYRA yet.

## Decision

Use the Morphe Manager application runtime as the integration baseline rather than copying only `patcher-core`. The patcher-core module is not a standalone APK patching engine: it expects the Morphe patcher library, ARSCLib, coroutine/serialization support, a prepared request, a patch-bundle loader, split-APK preparation, a runtime host, and an output/signing workflow.

The safest implementation path is a controlled upstream-derived integration on this feature branch, with VYRA's catalog and user experience layered on top. Keep the original upstream notices and clearly identify VYRA as a modified, separate product.

## Runtime components confirmed in upstream source

- `PatcherViewModel`: owns the patching UI state, selected app, patch selection/options, progress, completion, and export metadata.
- `PatcherWorker`: runs long operations through WorkManager, foreground progress/notifications, wake-lock handling, error reporting, and cleanup.
- `Runtime`: turns the selected bundle IDs/options into a serializable `PatchRequest`, prepares the device configuration, and starts the isolated patcher process.
- `PatcherService` / `PatcherProcesses`: hosts one patch run in a dedicated process and handles the ART or embedded-JVM execution path.
- `EmbeddedJvm` / JVM installation and native launcher: required for the embedded HotSpot path on supported ABIs.
- `PatchPipeline` / `Session`: prepares input splits, loads selected patches, applies them, writes the resulting APK, and handles temporary workspace cleanup.
- `SplitApkPreparer`: handles split APK archives and device-specific ABI/configuration selection.
- App-level repositories and installer/signing services: manage original APKs, patch bundles, keystores, exported APK metadata, and optional installation.

## VYRA integration sequence

1. **Pin and audit upstream inputs.** Pin exact Morphe Manager, Morphe Patcher, ARSCLib, and native-runtime revisions. Record licenses, notices, source URLs, and checksums. Do not use floating branches for release builds.
2. **Bring in the complete runtime.** Integrate the Android host/service/worker, patcher-core, JVM/native assets where required, split handling, and signing/output dependencies as one coherent build. Do not pretend patcher-core alone is sufficient.
3. **Adapt the application boundary.** Replace Morphe-specific app identity, navigation, catalog/repository entry points, and branding with VYRA equivalents while preserving required upstream notices and license disclosures.
4. **Connect the catalog safely.** Resolve a catalog entry to an exact package name, version, ABI/split requirements, patch bundle ID, publisher, release version, and SHA-256. Reject mismatched package/version/bundle metadata before execution.
5. **Add user controls.** Expose patch selection and patch options, then a VYRA output-customization step for display name and launcher icon only where the patch/runtime supports it. Never promise arbitrary icon/name modification for every target.
6. **Acquire inputs separately.** APK acquisition is not supplied by a patch catalog. Prefer official or otherwise authorized sources; verify package, version, signing certificate, split completeness, and hashes. Never silently substitute a different version.
7. **Execute with safeguards.** Run patching in the dedicated process/runtime with bounded storage and memory, cancellable WorkManager progress, clear logs, and cleanup. Community patch code is executable and must be treated as untrusted; keep unreviewed sources disabled by default.
8. **Verify output.** Confirm APK structure, package/version metadata, signer certificate, and installability. A modified APK is normally re-signed and therefore will not update an original app signed by a different certificate.
9. **Test before claiming support.** Start with a permissively licensed sample APK and a controlled test patch. Then test split APK input, cancellation, low-memory behavior, signing, and device installation. Only mark an app supported after an end-to-end test.

## Licensing and product identity

The inspected Morphe Manager source includes GPL-3.0 licensing and a NOTICE file with additional GPLv3 Section 7 terms. If VYRA distributes a combined or modified covered work, comply with GPL-3.0 obligations, provide corresponding source as required, retain copyright/license notices, and preserve the origin distinction. The NOTICE explicitly says the license grants no permission to use Morphe trademarks or logos. VYRA must use its own name, icon, and visual identity and must not imply that VYRA is an official Morphe product.

This is an engineering checklist, not legal advice. Confirm the exact license of every dependency and the distribution model before publishing binaries.

## Current implementation boundary

The current VYRA feature branch has catalog discovery, exact package/version candidate matching, verified MPP download by SHA-256, and read-only bundle inspection. It does not yet load or execute MPP code, run the Morphe patch pipeline, merge/sign a patched APK, or pass an end-to-end patch test. Keep those states explicit in the UI and release notes.
