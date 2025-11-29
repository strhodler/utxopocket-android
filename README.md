# UtxoPocket — privacy-first open-source watch-only wallet

UtxoPocket is an open-source Android app for monitoring multiple Bitcoin wallets via descriptors. Bundled Electrum nodes and onion endpoints route through Tor by default, local data is encrypted, and the entire flow is designed to stay fast, accessible, and low friction. Custom nodes now accept onion endpoints only, so every sync stays behind Tor.

---

## Value proposition
- **Privacy by default**: embedded Tor, zero telemetry, no clearnet fallbacks for curated or custom onion nodes.
- **Complete visibility**: clear dashboards plus wallet-level warnings whenever a sync misbehaves.
- **UX with intent**: adaptive Compose UI grounded in Material Design 3 + Expressive motion/shape/type guidance, responsive loaders (e.g., Tor bootstrap progress), intentional micro-interactions.
- **Reproducible & auditable**: deterministic build scripts and a strictly watch-only trust model.

### Acknowledgements
- Built with the [Hummingbird](https://github.com/sparrowwallet/hummingbird) UR toolkit by Sparrow Wallet (Apache-2.0).

---

## Installation
- Recommended: Install via [Obtainium](https://github.com/ImranR98/Obtainium) so updates track every tagged GitHub release. In Obtainium, add `https://github.com/strhodler/utxopocket-android`, pick the stable channel, and enable automatic download/notification to stay current.
- Manual: Download the latest `.apk` plus the matching `.sha256`/`.sha512` files from `app/release/` (or the GitHub Releases page), verify the checksum, then sideload with `adb install` or your device’s package installer.

### Verify downloads & keys
Use both the published checksums and the signing certificate fingerprints before sideloading:

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
- **Checksum validation:**
  ```bash
  sha256sum -c UtxoPocket-vX.Y.Z.apk.sha256
  UtxoPocket-vX.Y.Z.apk: OK

  sha512sum -c UtxoPocket-vX.Y.Z.apk.sha512
  UtxoPocket-vX.Y.Z.apk: OK
  ```
If any fingerprint or checksum deviates from the values above, treat the artifact as untrusted. The `keytool -list -v -keystore <keystore>.jks -alias <alias>` command prints the same signer fingerprints for reference. Android signing fingerprints are distinct from the maintainer’s PGP key (`BB15 B08E 943F 2391 D05E FC8F D9E9 1FB9 8800 D8FE`) in [`pubkey.asc`](pubkey.asc), used only for encrypted mail or detached signatures.

---

## Core capabilities
- Watch-only monitoring across multiple wallets with labels, UTXOs, and history.
- Private-by-default networking over Tor with curated nodes and custom onion endpoints. Tor remains mandatory; direct LAN/IP hosts and SSL toggles have been removed to keep behavior consistent.
- Multi-network support (Mainnet, Testnet3/4, Signet) with per-network presets.
- On-device health insights for transactions and UTXOs to spot reuse, dust, and risk.
- Fast onboarding and descriptor import (paste or QR), plus a searchable offline wiki.
- Safety controls: PIN lock with backoff, panic wipe, and encrypted local storage.
- Modern Compose UI (Material 3), responsive and accessible across screen sizes.

---

## Quick start
- Install the latest release and verify it (fingerprints + checksums).
- Open UtxoPocket and import your public descriptors (paste or scan QR).
- Select a test network (Signet/Testnet) in onboarding to trial safely; switch to Mainnet when ready.
- Connect via Tor, activate your preferred preset or onion node, and let the first sync complete; review wallet health.

---

## Descriptor compatibility
- Accepts standard public descriptors: `wpkh`, `sh(wpkh)`, `tr`, Miniscript (BDK‑supported), multisig `wsh(sortedmulti)`, and `addr(...)` exports.
- Supports BIP‑389 multipath (`/<0;1>/*`) and change branches; requires wildcard derivation and rejects any private‑key material.
- Full matrix and examples live in the project wiki; UtxoPocket remains strictly watch‑only.

---

## Bitcoin standards supported
- BIPs: 32/39 (HD), 43/44 (paths), 84/86 (SegWit/Taproot), 379 (Miniscript), 389 (multipath), 329 (labels). See the wiki for details.
- BIP-329 workflows: label transactions, inherit labels to UTXOs, toggle spendable/frozen state, and import/export JSONL so Sparrow and other apps stay in sync.

---

## Privacy & security
- Watch-only by design; no private keys or signing on device.
- Tor-backed networking for curated and custom onion nodes. Direct LAN/IP connectivity and SSL/TLS toggles were removed to enforce onion-only routing. See `SECURITY.md` for details and the privacy guarantees.

---

## Technology stack
- Build: Kotlin 2.2.21, JDK 21, AGP 8.13, Gradle VCs, KSP.
- UI: Jetpack Compose (Material 3 + Expressive motion/shape/type APIs where adopted), Navigation, Vico charts; bundled fonts (SIL OFL).
- Architecture: MVVM + Clean Architecture, coroutines/Flows, Hilt DI.
- Data & privacy: Room + SQLCipher, EncryptedFile + EncryptedSharedPreferences, DataStore.
- Bitcoin & networking: BDK 2.2.0, Tor + jtorctl, Electrum nodes, ZXing QR.
- Testing: JUnit, coroutines test, Room testing, Compose UI tests.

## Design system references
- Material component patterns are mirrored locally in `misc/material-components-android-master` (catalog, theming, motion, layout specs) and the Material 3 Expressive notes live in `misc/material-expresive` (expressive components, motion physics, typography, shape, and color tactics).
- Compose implementations should align with `UtxoPocketTheme` tokens (colors, typography, shapes) and the Expressive motion scheme before deviating with custom easing or radii.

---

## Credits & inspiration
- [BDK (Bitcoin Dev Kit)](https://github.com/bitcoindevkit)
- [Tor Project](https://github.com/torproject)
- [Sentinel](https://code.samourai.io/wallet/sentinel-android)
- [Ashigaru Wallet](https://ashigaru.rs/)
- [Sparrow Wallet](https://github.com/sparrowwallet/sparrow)
- [Hummingbird UR Toolkit](https://github.com/sparrowwallet/hummingbird)

---

Join telegram group: https://t.me/+6fel_1iKvQxmNzdk

---

## License
Released under the [MIT License](LICENSE). Third-party libraries bundled with UtxoPocket retain their original licenses.

---
