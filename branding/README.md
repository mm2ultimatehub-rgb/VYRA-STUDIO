# VYRA Studio branding

This directory contains the editable vector source for the VYRA symbol.

## Current assets
- `vyra-symbol.svg`: scalable vector symbol (512×512 viewBox).
- The separate PNG brand pack is delivered with this project conversation as `VYRA-STUDIO-BRAND-PACK.zip`.

## Android integration notes
- Use the PNG launcher artwork from the brand pack for legacy density launcher icons.
- The current launcher artwork is a single-layer square image. Before enabling Android adaptive icons, prepare a safe foreground layer and a solid background layer, then wire them through `mipmap-anydpi-v26/ic_launcher.xml`.
- Use `vyra-splash-1080x1920.png` as the visual reference for the launch screen; Android's native splash API generally expects a centered logo asset rather than a full-screen bitmap.
- Keep the application ID unchanged; branding must not alter package identity.
