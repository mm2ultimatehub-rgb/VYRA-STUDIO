# VYRA Studio branding

This directory contains the editable vector source for the VYRA symbol.

## Source asset
- `vyra-symbol.svg`: scalable vector symbol (512×512 viewBox).

## Android engine integration
The pinned Morphe engine generates its launcher activity aliases and adaptive icons from three foreground drawable resource names. The VYRA overlay replaces those three source resources in the temporary engine build:
- `ic_launcher_foreground.xml`: two-tone violet/cyan V mark.
- `ic_launcher_foreground_white.xml`: white monochrome V mark.
- `ic_launcher_foreground_black.xml`: black monochrome V mark.

The existing launcher icon generator continues to create the adaptive launcher icons and icon picker entries. The engine splash theme also references `@drawable/ic_launcher_foreground`, so it receives the VYRA mark through the same resource replacement.

The build overlay modifies only the temporary pinned upstream checkout; it does not rewrite the upstream submodule or its package identity. The engine application ID remains unchanged for Firebase compatibility.

The full-screen PNG splash image is a visual reference, not directly installed as a full-screen bitmap. Android's native splash screen uses the centered vector mark.
