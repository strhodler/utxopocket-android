# UtxoPocket

Privacy-first, open-source Android watch-only wallet for monitoring Bitcoin descriptors, analyzing UTXOs, and inspecting transaction flows.

## Features
- Watch-only monitoring for multiple wallets, balances, transactions, UTXOs, and labels
- Public descriptor import by paste or QR, including receive/change branches and multipath setups
- Broad watch-only compatibility across common BDK-supported singlesig, Taproot, Miniscript, multisig, and `addr(...)` exports
- BIP-329 label import/export workflows
- UTXO Canvas with collections, color tags, and automatic Dust grouping
- Analysis views for age, spendability, value bands, collections, and treemap inspection
- Transaction visualizer for inputs, outputs, change, and fee structure
- Encrypted watch-only `.ubak` backups with passphrase preview before restore
- Tor by default for bundled Electrum presets and custom onion endpoints (app-owned embedded runtime)
- Optional `Local Direct` mode for custom private/local IP literal Electrum endpoints
- Multi-network support for Mainnet, Testnet3, Testnet4, and Signet
- Early incoming detection while BDK sync remains the canonical wallet state
- Searchable offline wiki and glossary inside the app
- Optional PIN with backoff, duress PIN support, and calculator camouflage before PIN entry
- Panic wipe and encrypted local storage

## Values
- Privacy first: Tor is the default mode and the app ships with no analytics, crash reporting, or ad SDKs
- Watch-only by design: no seeds, no private keys, and no transaction signing on device
- Fail-closed networking: no silent fallback between Tor and `Local Direct`
- Open source and auditable: public codebase, transparent dependencies, and documented security posture
- Local security matters: encrypted storage, optional PIN protection, duress-aware access controls, and panic wipe support

## Get the app
- Install via [Obtainium](https://github.com/ImranR98/Obtainium) to track tagged GitHub releases
- Or download the latest APK, checksums, and release notes from [GitHub Releases](https://github.com/strhodler/utxopocket-android/releases)
- Release artifacts publish checksums and signing certificate fingerprints so you can verify what you install

## Built with
- [BDK / Bitcoin Dev Kit](https://github.com/bitcoindevkit)
- [Tor Project](https://www.torproject.org/) via Guardian Project `tor-android` + pinned `jtorctl`
- [Hummingbird UR Toolkit](https://github.com/sparrowwallet/hummingbird)

## Inspired by
- [Sparrow Wallet](https://github.com/sparrowwallet/sparrow)
- [Sentinel](https://code.samourai.io/wallet/sentinel-android)
- [Ashigaru Wallet](https://ashigaru.rs/)

## Build from source

Requirements: Java 21, Android SDK 37 (targetSdk 36), and an ARM64 Android device or emulator.

```bash
git clone https://github.com/strhodler/utxopocket-android.git
cd utxopocket-android
```

Create `local.properties`:

```text
sdk.dir=/absolute/path/to/Android/Sdk
```

Install the debug build:

```bash
./gradlew :app:installDebug
```

Run the main contributor checks:

```bash
./gradlew lintDebug
./gradlew :app:testDebugUnitTest
```

If dependency verification fails after updating dependency versions, regenerate the trusted hashes and commit `gradle/verification-metadata.xml` with the version catalog change:

```bash
./gradlew --write-verification-metadata sha256 :app:assembleDebug :app:testDebugUnitTest
```

For full setup, WSL notes, connected-device flows, and troubleshooting, see [`docs/project-setup.md`](docs/project-setup.md).

## Security and documentation
- Privacy and security posture: [`SECURITY.md`](SECURITY.md)
- Contributor workflow: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Setup and troubleshooting: [`docs/project-setup.md`](docs/project-setup.md)
- User onboarding guide: [`docs/getting-started.md`](docs/getting-started.md)

## License

UtxoPocket is released under the [MIT License](LICENSE). Bundled third-party components keep their original licenses; see [`NOTICE`](NOTICE).
