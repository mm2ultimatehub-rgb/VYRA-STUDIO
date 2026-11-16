# Repository index format — v1

VYRA repository indexes are declarative JSON documents. Parsing an index must never execute patch code.

## Example

```json
{
  "schemaVersion": 1,
  "repository": {
    "id": "community.example",
    "name": "Example Community",
    "description": "Community-maintained patch collection.",
    "homepage": "https://example.org"
  },
  "patches": [
    {
      "id": "sample-patch",
      "name": "Sample Patch",
      "description": "Example metadata only.",
      "target": {
        "packageName": "org.example.app",
        "minVersionCode": 120,
        "maxVersionCode": 180
      },
      "artifact": {
        "format": "mpp",
        "url": "https://example.org/releases/sample.mpp",
        "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      }
    }
  ]
}
```

## Required validation

- `schemaVersion` must be supported.
- Repository and patch IDs must be stable, unique identifiers.
- Patch IDs must be unique within one repository.
- Target package names are required.
- Version bounds, when supplied, must be non-negative and minimum must not exceed maximum.
- Artifact URLs must use HTTPS.
- Artifact SHA-256 must be a 64-character hexadecimal digest.
- Downloaded artifacts must be hashed and compared with the declared digest before they are offered to the patch engine.
- Index parsing and artifact download must never execute patch code.

## Trust model

An index is untrusted metadata until the user explicitly adds its source. HTTPS protects transport but does not prove that a repository owner is trustworthy. VYRA must show repository identity, origin URL, artifact digest, and target compatibility before any future patch execution.

The v1 format describes metadata and bundle locations only. It does not define a patch runtime or guarantee that an artifact can run on Android.
