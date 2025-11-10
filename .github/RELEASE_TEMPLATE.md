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
- **QA summary:** _Sentinel-run suites + manual probes (Tor, PIN, panic wipe)._
- **Automated suites:** ``./gradlew lintDebug`` • ``./gradlew :app:testDebugUnitTest`` • _Add others if executed (e.g., ``:app:connectedDebugAndroidTest``)._
- **Manual notes:** _Logs/screenshots if applicable._
- **Hash commands for users:**
  ```bash
  sha256sum UtxoPocket-vX.Y.Z.apk
  sha512sum UtxoPocket-vX.Y.Z.apk
  ```

## Artifacts
- 📦 `UtxoPocket-vX.Y.Z.apk` / `.aab`
- 🔐 `UtxoPocket-vX.Y.Z.apk.sha256`
- 🔐 `UtxoPocket-vX.Y.Z.apk.sha512`
- 📄 `SBOM-UtxoPocket-vX.Y.Z.json` (CycloneDX/SPDX) _or_ `deps-vX.Y.Z.txt`
- 🧪 `qa-report-vX.Y.Z.md` (optional but recommended)

## References
- Backlog items covered: _list issue numbers/links._
- Docs updated: _README/strings/etc._
- Security & privacy findings: _link to audit issue or note “no changes.”_
