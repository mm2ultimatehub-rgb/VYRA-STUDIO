# VYRA engine foundation branch

This branch is the working base for integrating VYRA product automation with the verified, pinned Morphe Manager runtime.

## Verified starting point

- Baseline branch: `integration/morphe-baseline`
- Baseline commit: `c1cdef437f0edef31dc5e02edbe68abeeddc4006`
- Morphe Manager pin: `d9f56746d13890c0f8837c6d070dae6f210ad2a9`
- Baseline CI run: https://github.com/mm2ultimatehub-rgb/VYRA-STUDIO/actions/runs/44029970228
- Result: upstream unit tests and debug APK assembly passed.
- Artifact: `morphe-upstream-baseline-debug`, approximately 72.5 MB.

The artifact is an upstream Morphe Manager baseline build, not a VYRA-branded release and not evidence of device-level patching validation.

## Integration objective

Bring the VYRA repository catalog, app discovery and safe download/verification workflow into this full runtime without replacing the upstream patch pipeline with a partial reimplementation.

## Ordered work

1. Keep the pinned upstream runtime and its license notices intact.
2. Establish a reproducible build on this branch before product changes.
3. Port VYRA's catalog client and compatibility models, resolving namespace and dependency conflicts deliberately.
4. Add catalog-driven patch bundle discovery while preserving the upstream patch selection and execution lifecycle.
5. Make automatic APK acquisition a separate provider layer. Prefer official or otherwise authorized sources; validate package, version, signing identity, split completeness and hashes before patching.
6. Expose VYRA's app-name and icon customization through supported upstream patch options.
7. Add execution safety controls: trusted-source policy, provenance, digest checks, bounded downloads, isolated patch process, cancellation and clear logs.
8. Build and run unit tests after each integration slice.
9. Validate with a legally usable sample APK and compatible test patch. Only then proceed to real supported applications and physical-device installation tests.

## Branch and release safety

- Do not merge into `main` automatically.
- Keep the standalone catalog-shell branch available as a reference until catalog functionality is ported and tested here.
- Do not remove upstream branding or notices until a complete VYRA identity and license-compliance review is ready.
- Never claim that patch execution, automatic APK acquisition, signing, or installation works until each path has passed an actual end-to-end test.

## Current status

This branch is an integration foundation. The successful upstream baseline CI is a prerequisite only. VYRA catalog integration, automatic APK sourcing, branding/customization and end-to-end patch tests remain to be implemented.
