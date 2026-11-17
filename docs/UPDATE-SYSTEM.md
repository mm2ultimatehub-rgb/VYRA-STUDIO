# VYRA STUDIO — Update system design

## Reference implementation

Morphe Manager includes two distinct update paths:
- Manager self-update: checks the manager release metadata, resolves a matching APK asset, downloads it, and hands installation to Android's installer.
- Patch-source updates: checks configured patch bundles and can notify when a newer bundle is available.

Its automatic-update policy separates checking from downloading:
- Download automatically (including mobile data)
- Download automatically on unmetered/Wi-Fi networks only
- Check and notify, but do not download
- Off

It also schedules periodic background checks with WorkManager and can show notifications for both manager and patch-bundle updates.

## VYRA implementation requirements

1. Publish VYRA releases through GitHub Releases and maintain machine-readable release metadata.
2. Check the latest stable release on launch and optionally on a WorkManager schedule.
3. Compare semantic version and Android version code; ignore drafts and prereleases unless the user opts in.
4. Display release notes, version, APK size, and SHA-256 before download.
5. Download only the official VYRA release asset over HTTPS; enforce a size limit and verify SHA-256 before exposing the APK to Android's package installer.
6. Use Android's package installer flow. Android may require user confirmation; VYRA must not claim silent installation unless a supported privileged installer is explicitly configured.
7. Offer user preferences equivalent to automatic download, Wi-Fi only, notify only, and off.
8. Keep manager updates independent from patch-bundle updates so either can be disabled separately.
9. Never replace the installed app from an untrusted mirror or a community patch repository.

## Current status

This document is an implementation plan. The current VYRA shell does not yet contain a working self-updater, release metadata endpoint, download flow, checksum verification UI, or update worker.
