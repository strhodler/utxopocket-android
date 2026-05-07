<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="96" alt="UtxoPocket logo" />

  # UtxoPocket

  Privacy-first Android watch-only wallet for Bitcoin descriptors, UTXOs, labels, and transaction flow inspection.

  [![Android Checks](https://img.shields.io/github/actions/workflow/status/strhodler/utxopocket-android/android-checks.yml?style=flat-square&label=Android%20checks)](https://github.com/strhodler/utxopocket-android/actions/workflows/android-checks.yml)
  [![Latest Release](https://img.shields.io/github/v/release/strhodler/utxopocket-android?style=flat-square&label=Release)](https://github.com/strhodler/utxopocket-android/releases)
  ![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7f52ff?style=flat-square&logo=kotlin&logoColor=white)
  ![JDK](https://img.shields.io/badge/JDK-21-437291?style=flat-square)
  ![minSdk](https://img.shields.io/badge/minSdk-28-3ddc84?style=flat-square&logo=android&logoColor=white)
  [![MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

  [Install](#install) • [Features](#features) • [Security Model](#security-model) • [Build](#build-from-source) • [Documentation](#documentation)
</div>

UtxoPocket helps you monitor Bitcoin wallets from public descriptors without turning your phone into a signer. It focuses on wallet visibility, UTXO review, private Electrum connectivity, and local-first education for users who want to inspect their wallet state without exposing signing material.

> [!IMPORTANT]
> UtxoPocket is watch-only by design. It never handles seeds, private keys, WIFs, `xprv`/`tprv` values, PSBT signing, transaction construction, or transaction finalization.

> [!NOTE]
> Tor is the default privacy boundary for bundled public Electrum presets and custom onion endpoints. `Local Direct` is an explicit opt-in mode for trusted private/local IP literal Electrum nodes only.

## Features

- Import public Bitcoin descriptors by paste or QR, including receive/change pairs and BIP-389 multipath exports.
- Monitor multiple wallets across Mainnet, Testnet3, Testnet4, and Signet.
- Inspect balances, transactions, UTXOs, labels, value bands, age, spendability, collections, and transaction flows.
- Organize coins with UTXO Canvas collections, color tags, automatic Dust grouping, histograms, and treemap views.
- Import and export BIP-329 labels, including QR-friendly workflows for portable wallet metadata.
- Export encrypted watch-only `.ubak` backups for descriptors, labels, collections, and selected local preferences.
- Use Tor-by-default Electrum sync, custom onion endpoints, or explicit `Local Direct` mode for trusted private/local infrastructure.
- Keep wallet data local with SQLCipher/Tink-backed storage, optional PIN, duress PIN, calculator camouflage, and panic wipe.
- Browse an offline Bitcoin wiki and glossary bundled into the app.
- Run without analytics, crash reporters, ad SDKs, explorer lookups, or remote attribution services.

## Install

- Download the latest APK, checksums, and release notes from [GitHub Releases](https://github.com/strhodler/utxopocket-android/releases).
- Or add this repository to [Obtainium](https://github.com/ImranR98/Obtainium) to follow tagged GitHub releases.
- Verify release artifacts with the published checksums and signing certificate fingerprints before installing.

Start with the [getting started guide](docs/getting-started.md) if this is your first descriptor-based watch-only wallet.

## Security Model

UtxoPocket treats privacy as a functional requirement:

- Private material is rejected even if a parser accepts the descriptor format.
- Tor mode fails closed if the Tor proxy is unavailable; there is no clearnet fallback while Tor mode is active.
- `Local Direct` rejects DNS names, `.local`, public IPs, onion endpoints, and public presets.
- Local data is encrypted, backups are passphrase protected, and panic wipe removes wallet, database, preference, Tor, cache, and key material artifacts.
- Network/Tor diagnostics are local, optional, and sanitized.

See [SECURITY.md](SECURITY.md) for the full threat model, storage design, permissions, backup semantics, and disclosure process.

## Build From Source

Requirements: JDK 21, Android SDK Platform 37, Android NDK 26+, and an ARM64 Android device or emulator for runtime testing.

```bash
git clone https://github.com/strhodler/utxopocket-android.git
cd utxopocket-android
```

Create an untracked `local.properties` file pointing to your Android SDK:

```text
sdk.dir=/absolute/path/to/Android/Sdk
```

Build the debug APK and run the fast contributor checks:

```bash
./gradlew :app:assembleDebug
./gradlew lintDebug :app:testDebugUnitTest
```

On Windows PowerShell, use `./gradlew.bat` or `.\gradlew.bat`. For device install flows, WSL notes, dependency verification, and troubleshooting, see [Project Setup For Contributors](docs/project-setup.md).

## Documentation

| Topic | Link |
| --- | --- |
| User onboarding | [Getting Started With UtxoPocket](docs/getting-started.md) |
| Contributor setup | [Project Setup For Contributors](docs/project-setup.md) |
| Testing guide | [How to Test UtxoPocket](docs/guides/how-to-test.md) |
| Connection reports | [Connection Troubleshooting & Reporting](docs/guides/connection-troubleshooting-reporting.md) |
| Security posture | [SECURITY.md](SECURITY.md) |
| Project docs index | [docs/README.md](docs/README.md) |

The in-app wiki and glossary are generated from Markdown under [`docs/wiki`](docs/wiki) and [`docs/glossary`](docs/glossary), then bundled as offline runtime assets.

## Built With

- [Kotlin](https://kotlinlang.org/), Gradle Kotlin DSL, Java 21, and a single Android `:app` module.
- Jetpack Compose, Material 3, Navigation Compose, Lifecycle ViewModel, Hilt, Coroutines, and Flow.
- Room, SQLCipher, Preferences DataStore, and Google Tink for local persistence and encryption.
- [BDK Android](https://bitcoindevkit.org/) for descriptor parsing, watch-only wallet state, persistence, and Electrum sync.
- Guardian Project `tor-android` plus pinned `jtorctl` for the embedded Tor runtime.
- [Hummingbird UR Toolkit](https://github.com/sparrowwallet/hummingbird) for descriptor and label QR workflows.

## Acknowledgements

UtxoPocket is inspired by the privacy, descriptor, and watch-only workflows in [Sparrow Wallet](https://github.com/sparrowwallet/sparrow), [Sentinel](https://code.samourai.io/wallet/sentinel-android), and [Ashigaru Wallet](https://ashigaru.rs/).
