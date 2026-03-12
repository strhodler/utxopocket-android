# SECURITY.md — UtxoPocket

UtxoPocket is a watch-only Android wallet that treats privacy as a functional requirement. This document describes the current security posture, how panic wipe works, and how to report issues.

## Security Posture
- **Watch-only trust model** — only public descriptors are accepted. Private keys, seeds, and transaction signing never touch the device.
- **Data at rest** — Room uses SQLCipher with a 64-byte passphrase encrypted with Tink AEAD and stored in app-private SharedPreferences (`secure_store_tink`). Tink keysets are bound to Android Keystore (`android-keystore://utxopocket-tink-master`), and crypto initialization fails closed if keystore-backed primitives are unavailable. BDK bundles are materialized only inside a temporary cache directory and sealed again with Tink StreamingAead as soon as the wallet persister releases.
- **Preferences** — Descriptor metadata, node settings, PIN material, and onboarding flags live in `Preferences DataStore`. Panic wipe clears the entire store and truncates the backing `user_preferences.preferences_pb` file to eliminate remnants.
- **Panic wipe coverage** — `Settings → Danger Zone → Panic wipe` now invalidates every wallet cache, deletes all Room tables inside a single transaction to avoid partial states if interrupted, removes encrypted BDK bundles, resets the SQLCipher database + key, clears strict Tink artifacts (`secure_store_tink`, `tink_keyset_prefs`, `tink_streaming_keyset_prefs`) plus legacy `secure_store` remnants, clears DataStore, wipes Tor state (`torfiles`), removes cache/ code-cache/ external-cache directories, and deletes the private network error log DB before restarting the app into the onboarding carousel.
- **PIN gate** — Optional 6-digit PIN is stored as PBKDF2(HMAC-SHA256, 150k iterations, 256-bit key) plus per-user salt. Verification enforces exponential backoff and temporary lockouts; panic wipe removes the hash/salt/counters.
- **Encrypted watch-only backups** — `Settings -> Wallets -> Backups -> Encrypted backup` exports `.ubak` files using PBKDF2(HMAC-SHA256) plus AES-256-GCM. Import requires passphrase-based preview and strict allowlist validation with forbidden-field rejection. Backups include watch-only descriptors/metadata (wallets, labels, collections, selected preferences) but exclude PIN/duress PIN material, node endpoint state, and any signing secrets.
- **Backup import semantics** — Restore is fail-closed and device-local: unsupported versions/security params, malformed payloads, forbidden fields, wrong passphrase, or tampering are rejected. Confirmed import replaces current local watch-only wallets and related metadata before rehydration.
- **Networking** — Tor is the default mode: bundled public presets and custom onion endpoints use the embedded Tor foreground service. Local Direct is optional and explicit opt-in for custom private/local IP literal endpoints only (IPv4/IPv6 literals; no DNS, `.local`, or hostnames), using `tcp://`. Mode/endpoint incompatibilities fail closed, and there is no automatic fallback between Tor and Local Direct. If Tor mode is active and Tor cannot supply a proxy, sync aborts (no clearnet fallback). Public preset failover remains manual (no automatic preset rotation). Custom node save validates the Electrum genesis hash for the selected network and blocks save on mismatch. Incoming detection follows the same active-mode transport policy; wallet truth remains canonical BDK sync.
- **No telemetry + private error logs** — The app does not ship analytics, crash reporters, or third-party ad SDKs. Optional network/Tor error logging is off by default and, when enabled, stores only sanitized metadata (masked/hashed hosts, operation, error class, timing, Tor state) inside the SQLCipher DB. User-visible connection errors and copied log exports redact raw endpoint host:port values (including Local Direct addresses). Entries are visible exactly as stored, can be copied locally, and can be wiped at any time; nothing leaves the device automatically.

## Threat Model Highlights
- **Device loss/coercion** — Panic wipe is designed for “wipe it now” scenarios. Because SQLCipher, BDK bundles, preferences, and Tor state are deleted before the app restarts, no remaining artifact can resurrect wallet metadata.
- **Offline brute force** — PIN protection slows shoulder-surf attackers using PBKDF2 + backoff, but secrets do not decrypt funds; descriptors remain public. Hardware signing remains the recommended second factor.
- **Backup file theft** — A stolen `.ubak` file is encrypted and integrity-checked, but security depends on backup passphrase quality and separate storage from the file.
- **Network observers** — Tor mode (default) protects endpoint privacy by routing traffic through Tor. Local Direct intentionally bypasses Tor for trusted private/local networks and can expose network metadata within that local path. Use Local Direct only for self-controlled infrastructure.
- **Data leakage** — The app sets `android:allowBackup="false"`, limits `FileProvider` scope, and keeps SQLCipher passphrases in encrypted preferences so Google backups or rooted filesystem snapshots cannot trivially recover wallet history.

## Runtime Permissions
- **Internet (`android.permission.INTERNET`)** and **network state (`android.permission.ACCESS_NETWORK_STATE`)**
  - Needed for Electrum connectivity and network-state awareness in both Tor and Local Direct modes. In Tor mode, sync aborts if Tor is unavailable (no clearnet fallback).
- **Foreground service (`android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_DATA_SYNC`)**
  - Keeps the Tor proxy alive while backgrounded and surfaces the mandatory persistent notification.
- **Camera (`android.permission.CAMERA`)**
  - Requested on demand for QR scanning (descriptors, endpoints). Frames are processed locally; no media capture is stored or transmitted.

Not requested: contacts, location, telephony, legacy storage, SMS, analytics, or ad permissions. Shared files (e.g., PDFs) use scoped `FileProvider`/MediaStore paths to avoid broad storage access.

## Dependencies of Interest
- [BDK Android 2.2.0](https://bitcoindevkit.org/)
- [SQLCipher 4.5.4](https://github.com/sqlcipher/sqlcipher)
- [Google Tink Android 1.20.0](https://github.com/tink-crypto/tink-java)
- [Tor Android binary 0.4.8.x + jtorctl](https://gitweb.torproject.org/)
- [Jetpack DataStore, Room, Compose, Hilt] (latest versions listed in `gradle/libs.versions.toml`)

Track CVEs for these dependencies and update via Gradle Version Catalogs. All release builds must pass `./gradlew lintDebug` and `./gradlew :app:testDebugUnitTest` before QA signs off.

## Reporting Issues
- **Private disclosures** — Email `strhodler@proton.me` or contact a maintainer privately for embargoed reports. Encrypt sensitive details with the maintainer PGP key stored at [`pubkey.asc`](pubkey.asc).
- **Public issues** — For low-risk bugs (copy errors, doc mismatches) open a GitHub issue and tag the maintainers so it gets triaged quickly.
- **PGP fingerprint** — `BB15 B08E 943F 2391 D05E FC8F D9E9 1FB9 8800 D8FE`. Verify the key with `gpg --show-keys --fingerprint pubkey.asc` before trusting signatures or sending encrypted mail.
- **Emergencies** — If Tor mode, Local Direct validation/fail-closed behavior, PIN, SQLCipher, or panic-wipe guarantees regress, pause releases and file an issue labeled `security-alert` so it is prioritized immediately.

We appreciate coordinated disclosure. Include reproduction steps, logs (with secrets removed), and affected commit/ build SHA so we can triage quickly.
