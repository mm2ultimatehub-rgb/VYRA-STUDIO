# VYRA STUDIO — App and Patch Catalog Research

Research date: 2026-11-17

## Product decision

VYRA should be app-first and source-agnostic. A user searches for an Android app (for example Discord, Reddit, Spotify, TikTok, Instagram, Telegram, YouTube, or YouTube Music). VYRA resolves the package identity, discovers patch bundles from trusted sources, checks exact APK version compatibility, obtains the matching APK from an approved distribution source, and then runs the selected patch pipeline. The user should not have to manually find an APK or an MPP file in the normal supported flow.

A catalog entry is not proof that a patch works with every release of that app. VYRA must show the exact supported package/version range and source/release provenance, and refuse or clearly warn on incompatible targets.

## Sources discovered

### 1. Morphe Patches (official)
- Repository: https://github.com/MorpheApp/morphe-patches
- Latest release observed during research: v2.3.0, published 2026-11-16.
- Release asset: patches-2.3.0.mpp
- Asset SHA-256 reported by GitHub release metadata: 1c37549274a2b6cf7f2e3e099d9b8cb641880b8a7dac323a95638a06bb4509a9
- README reports 212 patches total:
  - YouTube: 118
  - YouTube Music: 57
  - Reddit: 32
  - Universal: 5
- YouTube Music includes Custom branding options for app name and icon.
- Reddit includes a custom branding name patch.
- The README lists supported app versions; these must be parsed from the source metadata rather than hardcoded.

### 2. Morphe Community Patches index
- Catalog: https://morphe-patches.software/
- Current crawl/search result observed 2026-11-17: approximately 1,354 apps, 375 bundles, and 7,111 patches. Counts are live and can change.
- Examples surfaced in the catalog:
  - Instagram: 13 bundles / 242 patches
  - TikTok: 6 bundles / 212 patches
  - Discord: 3 bundles / 60 patches
  - Telegram: 8 bundles / 88 patches (additional separate Telegram entries also appear)
  - Spotify: 6 bundles / 23 patches
  - Facebook: 4 bundles / 152 patches
  - Twitter/X: 9 bundles / 139 patches
- The catalog is an index of third-party community sources. It does not mean Morphe itself reviews or guarantees those patches. Each bundle must be assessed independently.

### 3. Patch Bundle Registry
- Repository: https://github.com/Jman-Github/Patch-Bundle-Registry
- Describes a searchable catalog and read-only REST/GraphQL API spanning ReVanced, Morphe, and Reseam sources.
- Indexes sources, releases, patches, app compatibility, and public asset links. Bundle binaries remain hosted by publishers.
- Potentially useful as a discovery/metadata integration, subject to API stability, terms, provenance, and security review.

### 4. Additional source example
- https://github.com/kareemlukitomo/morphe-patches
- Publishes targeted Morphe-compatible bundles for Instagram, Threads, TikTok, Reddit, and X/Twitter, with stated target versions and signed release bundles.
- Treat as an independent third-party source, not as an official Morphe source.

## Proposed initial VYRA app catalog

Prioritize:
1. YouTube
2. YouTube Music
3. Reddit
4. Discord
5. Spotify
6. Instagram
7. TikTok
8. Telegram
9. Facebook
10. X / Twitter
11. Threads
12. Twitch
13. Pinterest
14. Messenger

Only mark an app as "supported" after at least one source has a verified bundle and exact compatible target version. Otherwise show "patch source found", "experimental", or "not currently supported" as appropriate.

## APK acquisition is a separate subsystem

Patch indexes and MPP bundles generally describe patch compatibility; they do not necessarily provide the original target APK. VYRA therefore needs a separate APK resolver:
- Prefer official publisher distribution channels or approved public repositories.
- Respect publisher terms, regional availability, and distribution restrictions.
- Do not re-host proprietary APKs in the VYRA repository or bundle them into VYRA releases without authorization.
- Verify package name, version, signer/certificate expectations, file digest, and split-APK completeness before patching.
- If no trustworthy compatible APK source exists, ask the user to provide the app rather than silently downloading from an unknown mirror.

## Security and trust requirements

MPP bundles can contain executable patch code. Community metadata is untrusted input:
- Keep a source allowlist/trust model and show publisher, repository, release, digest, and signature status.
- Verify SHA-256 and any available publisher signature before import.
- Do not load or execute bundles merely to inspect their metadata.
- Execute patching in the isolated patcher runtime/process, with resource limits, cancellation, and detailed logs.
- Distinguish official, community, experimental, stale, and unverified sources in the UI.
- Check patch dependencies/conflicts and exact target version before running.
- Never claim a patch works solely because an app appears in a community index.

## Implementation plan

1. Add an app catalog model keyed by Android package name, not display name.
2. Add discovery adapters for official Morphe metadata, the Community Patches index, and (after API review) Patch Bundle Registry.
3. Normalize app records, bundles, patch IDs, release channels, target versions, and provenance into one VYRA catalog.
4. Add a compatibility resolver that selects the correct bundle and APK version.
5. Add APK acquisition providers as a separate, policy-aware subsystem.
6. Integrate the full Morphe-derived patch runtime before enabling automatic patch execution.
7. Add end-to-end device tests for each app/version combination before promoting it to stable support.

## Current implementation status

This document records research and the intended architecture only. The current VYRA shell does not yet automatically crawl these catalogs, acquire APKs, execute patches, rebuild/sign patched APKs, or publish app-specific builds. Those capabilities must be implemented and tested before they are advertised as available.
