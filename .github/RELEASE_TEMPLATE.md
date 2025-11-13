## Highlights
### Added
- _Describe new capabilities; link issues/PRs._

### Fixed
- _List bug fixes or UX polish items._

### Security
- _Call out security/privacy hardening (PIN, Tor, SQLCipher, panic wipe, etc.)._

### Known issues
- _Document anything that still needs work or a follow-up issue._

## Verification
- **Version/Tag:** `vX.Y.Z`
- **Signer certificate fingerprints:**
  ```bash
  SHA-256: e5b195f0592cb546494df04722e9140e7dd92f4efd377ad8b159496d9bde9524
  SHA-1: 79e2591f07d8f439964ad320a3b8d1a2e4a75047
  ```
- **Signature verification:**
  ```bash
  apksigner verify --print-certs UtxoPocket-vX.Y.Z.apk

  Signer #1 certificate DN: ST=Blockchain, L=Mempool, O=strhodler, OU=strhodler, CN=strhodler
  Signer #1 certificate SHA-256 digest: e5b195f0592cb546494df04722e9140e7dd92f4efd377ad8b159496d9bde9524
  Signer #1 certificate SHA-1 digest: 79e2591f07d8f439964ad320a3b8d1a2e4a75047
  Signer #1 certificate MD5 digest: 918a3acf4d973633cc40a84949238536
  ```
- **Checksum:**
  ```bash
  sha256sum -c UtxoPocket-vX.Y.Z.apk.sha256
  UtxoPocket-vX.Y.Z.apk: OK

  sha512sum -c UtxoPocket-vX.Y.Z.apk.sha512
  UtxoPocket-vX.Y.Z.apk: OK
  ```

## Artifacts
- 📦 `UtxoPocket-vX.Y.Z.apk` / `.aab`
- 🔐 `UtxoPocket-vX.Y.Z.apk.sha256`
- 🔐 `UtxoPocket-vX.Y.Z.apk.sha512`
- 📄 `SBOM-UtxoPocket-vX.Y.Z.json` (CycloneDX/SPDX) _or_ `deps-vX.Y.Z.txt`

## References
- Backlog items covered: _list issue numbers/links._
- Docs updated: _README/strings/etc._
- Security & privacy findings: _link to audit issue or note “no changes.”_
