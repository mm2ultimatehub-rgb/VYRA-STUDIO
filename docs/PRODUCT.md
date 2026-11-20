# VYRA Store — Product Scope

## Vision

VYRA Store is an Android-first app catalog and update hub. Users can browse supported apps, open an app detail page, install a trusted VYRA build when one exists, and receive update notifications when a newer compatible release is published.

The first VYRA-modified app is **VYRA YouTube**. Spotify, Discord, Reddit, Telegram, and other apps may appear in the catalog, but a catalog listing is not a claim that VYRA already has a modified APK or an in-app installer for that app.

## Product components

- **VYRA Store:** user-facing app catalog, categories, search, app detail pages, downloads, and update center.
- **VYRA Repository:** machine-readable catalog of app identities, source provenance, release metadata, and availability status.
- **VYRA YouTube:** first VYRA-modified app and its versioned patch source/release pipeline.
- **VYRA Updater:** current standalone prototype for a YouTube update flow; it must be generalized and integrated into the Store before being treated as the multi-app update service.
- **VYRA Studio:** patching and creation workspace. The current Android app is an early shell; it is not yet a complete APK patcher.

## App availability states

- **Catalog only:** app identity and official store handoff are available; no verified VYRA APK or patch pipeline is published.
- **Patch candidate:** a compatible upstream/community patch source is listed, but VYRA has not completed an end-to-end build and device test.
- **Experimental:** a third-party or incomplete patch source needs manual review.
- **Verified VYRA build:** exact source APK and patch bundle have been applied, rebuilt, signed, installed, and smoke-tested; release metadata and hashes are published.

The app detail screen must show the actual state. It must never show an enabled VYRA download/update action when no verified APK asset exists.

## Core user flows

1. Browse or search the app catalog.
2. Open an app detail page and see its version, source, availability, changelog, and compatibility.
3. For catalog-only apps, open the official app store listing.
4. For a published VYRA build, download the APK from the verified release URL and verify its file size, SHA-256, package name, and signing certificate before handing it to Android's installer.
5. Check for updates on launch and periodically; show a notification only when a newer compatible release is available.
6. Keep Android's user-confirmed package installation flow. VYRA does not silently install apps.

## Signing and update compatibility

Android updates require the same package identity and a compatible signing certificate. VYRA must use a stable VYRA signing identity for every update of a VYRA-managed package. A modified app cannot replace an official app signed with a publisher's private key. Where the upstream signer cannot be preserved, VYRA must use a distinct package identity or clearly require a separate installation; it must not promise an in-place update of the official app.

Signing keys and passwords stay in GitHub Actions secrets or another protected signing service. They are never committed to source control or shipped inside the app.

## First release scope

- Complete the real VYRA YouTube patch/build/sign/install test before publishing a VYRA YouTube APK.
- Keep YouTube as the first end-to-end supported app.
- Show Spotify, Discord, Telegram, Reddit, and other apps with truthful catalog/patch availability.
- Connect the central VYRA Repository to the Store UI.
- Replace the YouTube-only updater prototype with a per-app update manifest and verified installer pipeline.

## Current implementation boundary

Implemented in the existing repositories:
- Central app catalog and source metadata in VYRA Repository.
- Initial app identities for YouTube, YouTube Music, Reddit, Instagram, TikTok, Threads, X, Spotify, Discord, Telegram, Facebook, Twitch, Pinterest, and Messenger.
- Explicit catalog-only status for apps without a reviewed VYRA patch bundle.
- VYRA YouTube patch overlay and CI that builds a Morphe-compatible `.mpp` patch bundle.
- A separate Android updater prototype that currently targets YouTube only.

Not implemented or not yet verified:
- A complete multi-app Store UI connected to the central catalog.
- A real VYRA YouTube APK generated from a clean input APK.
- End-to-end patch execution, signing, install, and device smoke test in the current VYRA pipeline.
- Generic per-app update manifests, background checks, and notifications in the Store.
- Multi-app installation/update support.

A successful `.mpp` build is a patch bundle, not an installable APK. A successful updater build is the updater app, not VYRA YouTube.
