# Building the VYRA YouTube patch bundle

This repository now contains a source overlay and a GitHub Actions workflow that builds a
Morphe-compatible `.mpp` patch bundle from the pinned upstream source.

## What the workflow does

1. Checks out Morphe Patches commit `31234a8465683a389bcfba25ad636fd1cd38fd02`.
2. Applies `tools/apply_vyra_youtube_overlay.py`.
3. Moves the existing Glass navigation and navigation-animation preferences into a
   VYRA Appearance screen while retaining the preference keys consumed by the upstream
   Glass renderer.
4. Renames the visible root settings title to **VYRA MOD**.
5. Builds `:patches:buildAndroid` and uploads the resulting `.mpp` file as an artifact.

The overlay intentionally fails if the pinned source layout or resource anchors change.

## One-time GitHub setup

The Morphe Gradle plugin is hosted in GitHub Packages. Add a repository Actions secret named
`MORPHE_GITHUB_TOKEN` containing a GitHub token with `read:packages` access. Do not put the
token in source code, workflow YAML, or a committed Gradle properties file.

Then open **Actions → Build VYRA YouTube patch bundle → Run workflow** on branch
`vyra-youtube-mod`. Download the `vyra-youtube-patch-bundle` artifact from the completed run.

## Applying the bundle

Import the generated `.mpp` patch bundle into Morphe Manager, select the matching YouTube
21.30.216 APK and apply the patches. The original APK is not uploaded by this workflow and is
not stored in this repository.

## Current boundary

This workflow builds a patch bundle, **not a ready-to-install YouTube APK**. The bundle still
needs to be applied to the user's original APK and the resulting APK must be signed and tested
on a device. The existing VYRA-STUDIO Android CI builds the companion workspace app only.
