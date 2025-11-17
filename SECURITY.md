# SECURITY.md — UtxoPocket

UtxoPocket is a watch-only Android wallet that treats privacy as a functional requirement. This document describes the current security posture, how panic wipe works, and how to report issues.

## Security Posture
- **Watch-only trust model** — only public descriptors are accepted. Private keys, seeds, and transaction signing never touch the device.
- **Data at rest** — Room uses SQLCipher with a 64-byte passphrase kept in Jetpack Security `EncryptedSharedPreferences`. BDK bundles are decrypted only inside a temporary cache directory and sealed again with `EncryptedFile` as soon as the wallet persister releases.
- **Preferences** — Descriptor metadata, node settings, PIN material, and onboarding flags live in `Preferences DataStore`. Panic wipe clears the entire store and truncates the backing `user_preferences.preferences_pb` file to eliminate remnants.
- **Panic wipe coverage** — `Settings → Danger Zone → Panic wipe` now invalidates every wallet cache, deletes all Room tables, removes encrypted BDK bundles, resets the SQLCipher database + key, clears DataStore, wipes Tor state (`torfiles`), and removes cache/ code-cache/ external-cache directories before restarting the app into the onboarding carousel.
- **PIN gate** — Optional 6-digit PIN is stored as PBKDF2(HMAC-SHA256, 150k iterations, 256-bit key) plus per-user salt. Verification enforces exponential backoff and temporary lockouts; panic wipe removes the hash/salt/counters.
- **Networking** — Bundled public nodes and onion endpoints always ride over the embedded Tor foreground service. Custom host/IP nodes can opt into direct connections for LAN/WireGuard deployments; Tor is skipped when you select those entries. TLS is enforced by default on host/IP nodes, but advanced users can explicitly disable SSL/TLS when pointing at plaintext Electrum ports (the UI warns loudly). If Tor cannot supply a proxy for Tor-required nodes, syncs abort instead of falling back to clearnet. Users may pick bundled public nodes or define onion/custom endpoints per network.
- **No telemetry** — The app does not ship analytics, crash reporters, or third-party ad SDKs. Logs stay local and are scoped to debugging Tor/node state.

## Threat Model Highlights
- **Device loss/coercion** — Panic wipe is designed for “wipe it now” scenarios. Because SQLCipher, BDK bundles, preferences, and Tor state are deleted before the app restarts, no remaining artifact can resurrect wallet metadata.
- **Offline brute force** — PIN protection slows shoulder-surf attackers using PBKDF2 + backoff, but secrets do not decrypt funds; descriptors remain public. Hardware signing remains the recommended second factor.
- **Network observers** — Tor remains mandatory for bundled public nodes and onion endpoints, while custom host/IP nodes can opt out for trusted LAN/WireGuard environments. TLS validation leverages BDK’s Electrum client. Custom node entries allow onion-only setups to avoid exit nodes entirely; direct hosts should only be used when the network path is already private.
- **Data leakage** — The app sets `android:allowBackup="false"`, limits `FileProvider` scope, and keeps SQLCipher passphrases in encrypted preferences so Google backups or rooted filesystem snapshots cannot trivially recover wallet history.

## Runtime Permissions
- **Internet (`android.permission.INTERNET`)** and **network state (`android.permission.ACCESS_NETWORK_STATE`)**
  - Needed to route Electrum traffic through Tor (for bundled/onion nodes) or over direct sockets (for trusted LAN/IP nodes) and to react to connectivity changes; Tor-required syncs abort instead of falling back to clearnet.
- **Foreground service (`android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_DATA_SYNC`)**
  - Keeps the Tor proxy alive while backgrounded and surfaces the mandatory persistent notification.
- **Camera (`android.permission.CAMERA`)**
  - Requested on demand for QR scanning (descriptors, endpoints). Frames are processed locally; no media capture is stored or transmitted.

Not requested: contacts, location, telephony, legacy storage, SMS, analytics, or ad permissions. Shared files (e.g., PDFs) use scoped `FileProvider`/MediaStore paths to avoid broad storage access.

## Dependencies of Interest
- [BDK Android 2.2.0](https://bitcoindevkit.org/)
- [SQLCipher 4.5.4](https://github.com/sqlcipher/sqlcipher)
- [Jetpack Security Crypto 1.1.0](https://developer.android.com/topic/security/data)
- [Tor Android binary 0.4.8.x + jtorctl](https://gitweb.torproject.org/)
- [Jetpack DataStore, Room, Compose, Hilt] (latest versions listed in `gradle/libs.versions.toml`)

Track CVEs for these dependencies and update via Gradle Version Catalogs. All release builds must pass `./gradlew lintDebug` and `./gradlew :app:testDebugUnitTest` before QA signs off.

## Reporting Issues
- **Private disclosures** — Email `strhodler@proton.me` or contact a maintainer privately for embargoed reports. Encrypt sensitive details with the maintainer PGP key stored at [`pubkey.asc`](pubkey.asc).
- **Public issues** — For low-risk bugs (copy errors, doc mismatches) open a GitHub issue and tag the maintainers so it gets triaged quickly.
- **PGP fingerprint** — `BB15 B08E 943F 2391 D05E FC8F D9E9 1FB9 8800 D8FE`. Verify the key with `gpg --show-keys --fingerprint pubkey.asc` before trusting signatures or sending encrypted mail.
- **Emergencies** — If Tor, PIN, SQLCipher, or panic-wipe guarantees regress, pause releases and file an issue labeled `security-alert` so it is prioritized immediately.

We appreciate coordinated disclosure. Include reproduction steps, logs (with secrets removed), and affected commit/ build SHA so we can triage quickly.
