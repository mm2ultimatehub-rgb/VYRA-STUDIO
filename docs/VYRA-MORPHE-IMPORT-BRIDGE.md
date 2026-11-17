# VYRA → Morphe local MPP import bridge

## Purpose

The VYRA catalog/downloader and the Morphe patch engine are separate Android
applications/modules in this repository. This overlay adds a narrow bridge
inside the pinned Morphe Manager runtime so a verified MPP file can be imported
through Morphe's own local-bundle repository.

## Pinned upstream

The overlay refuses to apply unless `upstream/morphe-manager` is exactly:

`d9f56746d13890c0f8837c6d070dae6f210ad2a9`

This keeps the integration tied to the source revision that was inspected.

## Import sequence

1. The caller supplies a downloaded MPP file and the expected catalog SHA-256.
2. The bridge checks the digest format, file existence, and 50 MB compressed-size limit.
3. It opens the MPP as a JAR and checks for a manifest and at least one DEX payload.
4. It rejects archives with more than 50,000 entries, duplicate or unsafe paths,
   Windows-style traversal, and actual decompressed content above 250 MB. The validator
   streams every entry instead of trusting ZIP size metadata.
5. It computes SHA-256 from the file bytes and compares it with the expected digest.
6. Only after all checks pass, it calls Morphe's `PatchBundleRepository.createLocal()`.
7. Morphe owns the resulting local source and its normal patch metadata loading.

## Catalog execution gate\n\nThe repository currently marks its app entries as candidate/experimental and its\npatch sources as disabled-until-reviewed. Therefore the catalog service can\ndiscover compatible entries but intentionally refuses to import them. A matching\nversion and valid SHA-256 are not, by themselves, permission to execute a patch\nbundle. The gate requires app status `verified`, source trust\n`official-publisher`, and execution policy `enabled`.\n\n## What this does not do

- It does not download APKs.
- It does not automatically choose or execute patches.
- It does not make an unsupported app compatible.
- It does not prove a publisher is trustworthy merely because a digest matches.
- It does not replace the need for a trusted-source policy and user-visible consent.
- It does not yet combine the standalone VYRA shell/catalog UI and Morphe Manager UI into one distributable app.

The catalog currently labels its entries candidate/experimental and its execution
policy disabled, so automatic import remains blocked. A successful import in a future
approved configuration will mean Morphe accepted the local MPP as a source; it is not
proof that a particular APK was patched successfully.

## Overlay application

`scripts/apply-vyra-morphe-overlay.sh` copies the bridge into the pinned
upstream source tree and adds its constructor to Koin's repository module.
`scripts/verify-upstream-baseline.sh` applies this overlay before running the
Morphe unit tests and debug APK build.

The overlay modifies only the CI working copy of the submodule. It does not
change the pinned upstream commit or write to the Morphe upstream repository.

The upstream verification workflow also downloads the official Morphe 2.3.0 MPP
into its ephemeral workspace, verifies its pinned SHA-256, and runs the archive
validator against that real release artifact. The binary is not committed to VYRA.

## Licensing and attribution

Morphe Manager remains GPL-3.0. Its upstream license, NOTICE file, copyright
headers, and required attribution must be preserved in distributed builds.
VYRA branding must not imply that Morphe authors endorse VYRA.
