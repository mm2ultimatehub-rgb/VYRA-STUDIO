# Installed APK acquisition

VYRA's patcher can now copy an app's installed base APK into its private cache when the user selects an app from the catalog and chooses **Use installed app**.

## Safety and current limitations

- The app package must be one of the explicitly declared catalog packages in the manifest.
- VYRA copies the installed base APK; it does not fetch an APK from an arbitrary mirror.
- The copied file is passed through the existing APK inspector, which reads package/version metadata and calculates SHA-256 and signer fingerprints.
- Split installs are deliberately rejected at this stage. A base APK alone is not a complete install set, so VYRA will not pretend that it can patch a split package correctly.
- Apps not installed on the device still require a user-selected APK. A trusted remote APK source and complete split-set downloader have not yet been implemented.
- Copying an installed APK does not bypass Android signature rules. A rebuilt APK signed with a different key generally cannot update the original app in place.

The copied APK lives in VYRA's private cache and is not uploaded.
