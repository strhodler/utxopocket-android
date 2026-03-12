# UtxoPocket — privacy-first open-source watch-only wallet

UtxoPocket is an open-source Android app for monitoring multiple Bitcoin wallets via descriptors. Tor is the default connection mode for bundled Electrum presets and custom onion endpoints, local data is encrypted, and the entire flow is designed to stay fast, accessible, and low friction. Local Direct is optional for custom private/local IP literal endpoints only (no DNS, `.local`, or hostnames), with no automatic fallback between modes.

---

## Value proposition
- **Privacy-first connection posture**: Tor is default, Local Direct is explicit opt-in for trusted private/local networks, zero telemetry, and no silent fallback between modes.
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
- Home-screen UTXO canvas with drag-and-drop collections, color tags, and automatic Dust grouping.
- Encrypted watch-only backup export/import (`.ubak`) with passphrase-protected preview before import.
- Mode-aware networking: Tor (default) supports curated nodes and custom onion endpoints; Local Direct (optional) supports only custom private/local IP literals over `tcp://`.
- Fail-closed connection policy: incompatible mode/endpoint combinations are blocked, and the app never auto-switches between Tor and Local Direct.
- Connection retries do not auto-rotate bundled presets; switching presets remains an explicit user action.
- Custom node save validates the Electrum genesis hash for the selected app network and blocks mismatches.
- Multi-network support (Mainnet, Testnet3/4, Signet) with per-network presets.
- Incoming detection uses a lightweight Electrum watcher on the active connection mode for early `unconfirmed`/`confirmed-light` signals, while BDK sync remains the canonical source for wallet state.
- Incoming placeholders have no time expiration; they are removed only after a successful BDK sync reconciles the txid.
- Per-wallet Analysis section with age distribution and hold-wave views to assess how long coins have been sitting and where value concentrates.
- Fast onboarding and descriptor import (paste or QR), plus a searchable offline wiki.
- Safety controls: PIN lock with backoff, panic wipe with atomic DB wipe + storage removal, and encrypted local storage.
- Modern Compose UI (Material 3), responsive and accessible across screen sizes.

---

## Quick start
- Install the latest release and verify it (fingerprints + checksums).
- Open UtxoPocket and import your public descriptors (paste or scan QR).
- Select a test network (Signet/Testnet) in onboarding to trial safely; switch to Mainnet when ready.
- Keep Tor mode (default) for bundled presets/onion nodes, or explicitly switch to Local Direct for your own private/local IP literal node; then let the first sync complete.
- Create an encrypted `.ubak` backup from `Settings -> Wallets -> Backups`, and store the file and backup passphrase separately.

---

## Run from source
- Prerequisites: JDK 21, Android SDK 36, and a physical ARM64 device (or ARM64 emulator).
- Project bootstrap:
  ```bash
  git clone https://github.com/strhodler/utxopocket-android.git
  cd utxopocket-android
  ```
- Set `local.properties` with your SDK path:
  ```text
  sdk.dir=/absolute/path/to/Android/Sdk
  ```
- Build and install from Linux/macOS:
  ```bash
  ./gradlew :app:installDebug
  adb shell am start -n com.strhodler.utxopocket/.presentation.MainActivity
  ```
- WSL + USB fallback (when Linux `adb devices` is empty):
  ```bash
  ./gradlew :app:assembleDebug
  "/mnt/c/Users/<windows-user>/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/debug/app-debug.apk"
  "/mnt/c/Users/<windows-user>/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.strhodler.utxopocket/.presentation.MainActivity
  ```
- Full contributor setup and troubleshooting: `docs/project-setup.md`.

---

## Documentation model
- Public and in-app docs live in `docs/` (guides, wiki, glossary).
- Internal project context for engineering and LLM workflows lives in `knowledge/` (English only).
- Local docs stewardship commands are available in OpenCode:
  - `docs-impact` to analyze what documentation should change for current code diffs.
  - `docs-sync` to draft and apply documentation updates as current-state truth.

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
- Tor-default networking for curated presets and custom onion nodes, plus optional Local Direct for private/local IP literal custom nodes. No silent fallback is applied between modes. See `SECURITY.md` for constraints and privacy guarantees.
- Encrypted `.ubak` backups are watch-only by scope: they restore descriptors and local metadata, but never include seeds/private keys or PIN/duress PIN material.

---

## Technology stack
- Build: Kotlin 2.2.21, JDK 21, AGP 8.13, Gradle VCs, KSP.
- UI: Jetpack Compose (Material 3 + Expressive motion/shape/type APIs where adopted), Navigation, Vico charts; bundled fonts (SIL OFL).
- Architecture: MVVM + Clean Architecture, coroutines/Flows, Hilt DI.
- Data & privacy: Room + SQLCipher, strict keystore-backed Tink (AEAD + StreamingAead, fail-closed), DataStore.
- Bitcoin & networking: BDK 2.2.0, Tor + jtorctl, Electrum nodes, ZXing QR.
- Testing: JUnit, coroutines test, Room testing, Compose UI tests.

## Design system references
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
