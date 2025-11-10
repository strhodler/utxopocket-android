# UtxoPocket — privacy-first open-source watch-only wallet

UtxoPocket is an open-source Android app for monitoring multiple Bitcoin wallets via descriptors. Every network call is routed through Tor, local data is encrypted, and the entire flow is designed to stay fast, accessible, and low friction.

---

## Installation
- Recommended: Install via [Obtainium](https://github.com/ImranR98/Obtainium) so updates track every tagged GitHub release. In Obtainium, add `https://github.com/strhodler/utxopocket-android`, pick the stable channel, and enable automatic download/notification to stay current.
- Manual: Download the latest `.apk` plus the matching `.sha256`/`.sha512` files from `app/release/` (or the GitHub Releases page), verify the checksum, then sideload with `adb install` or your device’s package installer.

---

## Value proposition
- **Privacy by default**: embedded Tor, zero telemetry, no clearnet fallbacks.
- **Complete visibility**: clear dashboards plus wallet-level warnings whenever a sync misbehaves.
- **UX with intent**: adaptive Compose UI, responsive loaders (e.g., Tor bootstrap progress), intentional micro-interactions.
- **Reproducible & auditable**: deterministic build scripts and a strictly watch-only trust model.

---

## Core capabilities
- Guided onboarding with network selection and Tor/Electrum connectivity checks.
- Descriptor registration (manual entry or QR scan) with multi-wallet management.
- Wallet list that surfaces balance, transaction count, last sync, and status badges per card.
- Wallet detail screen with sortable transaction + UTXO catalogs, address reuse counters, dust/change insights, QR exports, per-output labeling, and an interactive balance history chart.
- Health analytics (Transaction, UTXO, and Wallet health pillars) that run fully on-device with opt-in toggles and explainable heuristics.
- Global status hub for Tor and node health, including bootstrap progress, proxy diagnostics, Tor identity renewal, and Electrum metadata.
- Security controls: PIN gate hashed with PBKDF2 + exponential backoff, customizable units/themes, panic wipe that clears wallets/transactions/BDK files, and descriptor sharing flags that widen scans when needed.
- BDK wallet bundles are decrypted only inside a temporary workspace and re-encrypted with Jetpack Security’s `EncryptedFile` layer (database + WAL + SHM) as soon as the persister closes, so watch-only descriptors never sit unprotected on disk.
- Multi-network support (Mainnet, Testnet3, Testnet4, Signet) with dedicated Electrum presets and network-aware retry policies.
- Custom node directory (public presets, host/port, onion) with per-network retries, plus a searchable offline wiki + glossary for quick references.

---

## Descriptor compatibility
- BDK 2.2.0 fully supports [BIP-389](https://github.com/bitcoin/bips/blob/master/bip-0389.mediawiki) multipath descriptors (e.g., `/…/<0;1>/*`), and UtxoPocket now imports them directly without rewriting the descriptor or prompting for a split.
- Supported templates include single-key `wpkh(...)`, wrapped SegWit `sh(wpkh(...))`, taproot `tr(...)`, Miniscript policies covered by [BIP-379](https://github.com/bitcoin/bips/blob/master/bip-0379.mediawiki), multisig `wsh(sortedmulti(...))`, and watch-only `addr(...)` exports. Anything outside BDK’s descriptor set (e.g., private-key-bearing strings) is rejected with a clear error.
- When the descriptor relies on additional metadata (policy labels, signer roles), we display a warning and ask you to confirm that the imported policy matches what BDK can enforce. We no longer auto-rewrite advanced descriptors into simpler forms.
- HD descriptors must include wildcard derivation (`*`) and either a matching change branch or a BIP-389 multipath tuple (`/<0;1>/*`). Single fixed-address templates such as `wpkh(pubkey)` are not supported unless they are wrapped in `addr(...)`. Consult the descriptor-support appendix in the project wiki for the full compatibility matrix.
- UtxoPocket is watch-only by design: descriptors must be public, and providing the change branch keeps balances and change analysis accurate even when spending happens on external devices.
- **Shared descriptors**: keep this toggle on when the same descriptor pair is watched by more than one device or app. UtxoPocket bumps the address gap limit, schedules a full rescan whenever the flag changes, and periodically refreshes the address pool so coins discovered elsewhere still appear in sync. Turn it off only if UtxoPocket is the exclusive watcher—doing so avoids unnecessary wide scans while keeping the local gap limit tight.

---

## Bitcoin standards supported
- **BIP-32 / BIP-39** — HD key derivations are preserved, so any mnemonic + derivation path combo you import will match your hardware or desktop wallet.
- **BIP-43/44 families** — Legacy, nested, and custom purpose paths are parsed from descriptors and surfaced in address detail views.
- **BIP-84 & BIP-86** — Native SegWit and Taproot descriptors (`wpkh`, `tr`) sync without rewriting change branches.
- **BIP-379 & Miniscript** — Policies that compile to supported scripts are accepted, analyzed, and labeled correctly.
- **BIP-389** — Multipath descriptors (`/<0;1>/*`) eliminate the need to paste separate external/change strings.
- **BIP-329** — Wallet/transaction/UTXO labels can be exported as JSON Lines compatible with the BIP-329 reference format for syncing across apps.

---

## Technology stack
- **Language & build**: Kotlin 2.2.21 with Compose compiler, JDK 21 toolchain, AGP 8.13, Gradle Version Catalogs, and KSP for annotation processing.
- **UI**: Jetpack Compose + Material 3 surfaces, declarative navigation, and Vico charts for balance history.
- **Typography**: Encode Sans (body) and Encode Sans Expanded (display), bundled under the SIL OFL 1.1, keep the interface legible and brand-consistent across devices without contacting third-party services.
- **Architecture**: MVVM + Clean Architecture powered by coroutines/Flows, Hilt DI, and unidirectional state holders.
- **Data & privacy**: Room + SQLCipher, Jetpack Security `EncryptedFile`, EncryptedSharedPreferences, and DataStore for preferences.
- **Bitcoin & networking**: BDK 2.2.0 for descriptor sync, Tor + `jtorctl`, Electrum endpoint management with custom node presets, and ZXing for QR I/O.
- **Testing & automation**: JUnit, coroutines test kit, Room in-memory tests, Compose UI testing, and reusable Gradle tasks for lint/unit checks.

---

## Maintainer Contact & PGP
- Email `strhodler@proton.me` for coordinated disclosures or release verification details; encrypt sensitive reports with the maintainer key in [`pubkey.asc`](pubkey.asc).
- Fingerprint: `BB15 B08E 943F 2391 D05E FC8F D9E9 1FB9 8800 D8FE` (import/verify with `gpg --show-keys --fingerprint pubkey.asc`).
- The same key will sign future security responses or release artifacts that require manual verification.

---

## Wiki and Glossary Content
- Source files: Markdown under `docs/wiki` and `docs/glossary` are packaged as app assets via `app/build.gradle.kts`:72.
- Merge model:
  - Wiki: Markdown topics are parsed and merged over built‑in topics; matching `id` replaces the default. Categories are created or updated from front‑matter.
  - Glossary: Markdown entries are parsed and merged over built‑in entries by `id`, then sorted alphabetically by term.
- Required front‑matter
  - Wiki (`docs/wiki/.../*.md`): `id`, `title`, `summary`. Optional: `category_id`, `category_title`, `category_description`, `keywords`, `related`, `glossary_refs`. Use `##` headings for section titles.
  - Glossary (`docs/glossary/*.md`): `id`, `title`, `summary`. Optional: `aliases`, `keywords`. The body becomes definition paragraphs.
- How to add/update content
  1) Drop or edit `.md` files in `docs/wiki` or `docs/glossary` with valid front‑matter.
  2) Build/run the app; the Markdown is read from assets at runtime and shown in the Wiki/Glossary.
  3) Keep English as the canonical source; translate later if needed.

---

## System Permissions
- Internet (`android.permission.INTERNET`) and Network state (`android.permission.ACCESS_NETWORK_STATE`)
  - Required to route all Electrum traffic over Tor and to react to connectivity changes.
  - There is no clearnet fallback; sync aborts if Tor is unavailable.
- Foreground service (`android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_DATA_SYNC`)
  - Used by the Tor foreground service so connectivity remains stable while the app is in the background.
  - The service shows a persistent notification as required by Android.
- Camera (`android.permission.CAMERA`)
  - Requested at runtime only when you open the QR scanner (e.g., to import descriptors or endpoints).
  - Not used for photos or video; frames are processed locally for QR decoding only.

Not requested: contacts, location, phone, storage (legacy), SMS, or analytics/ads permissions. Files shared (e.g., the Whitepaper PDF) use scoped `FileProvider`/MediaStore without broad storage access.

---

## Credits & inspiration
- [BDK (Bitcoin Dev Kit)](https://github.com/bitcoindevkit)
- [Tor Project](https://github.com/torproject)
- [Sentinel](https://code.samourai.io/wallet/sentinel-android)
- [Ashigaru Wallet](https://ashigaru.rs/)
- [Sparrow Wallet](https://github.com/sparrowwallet/sparrow)

---

## License
Released under the [MIT License](LICENSE). Third-party libraries bundled with UtxoPocket retain their original licenses.

---
