# Project Setup For Contributors

Follow these steps to run UtxoPocket from source, reproduce the CI checks, and install debug builds on devices.

## 1. Requirements
- Linux, macOS, or Windows Subsystem for Linux.
- Git, `adb`, and Java **21** (Temurin/OpenJDK). `java -version` should report 21.x.
- Android Studio Koala (or newer) with:
  - Android SDK Platform **36** + build tools (matches `compileSdk/targetSdk = 36`)
  - Android NDK **26+**
  - HAXM/ARM64 image support if you rely on emulators
- Physical ARM64 device (Android 10+) or an ARM64 emulator (Tor binary does not include x86 builds).

## 2. Clone & Bootstrap
```bash
git clone https://github.com/strhodler/utxopocket-android.git
cd utxopocket-android
```
Create `local.properties` (not tracked in git) pointing to your SDK:
```
sdk.dir=/absolute/path/to/Android/Sdk
```
Android Studio can populate this automatically (File → Sync Project with Gradle Files). Optional: enable `direnv` or similar tooling to auto-load environment variables per repo.

## 3. Run Lint & Unit Tests
Before opening a PR, run the fast feedback loop:
```bash
./gradlew lintDebug
./gradlew :app:testDebugUnitTest
```
Touching Tor, networking, or Compose UI? also run:
```bash
./gradlew :app:connectedDebugAndroidTest
```

## 4. Install The Debug Build
```bash
adb devices             # ensure your target shows “device”
./gradlew :app:installDebug
```
Launch the “UtxoPocket” icon, complete onboarding, and verify Tor bootstrap. Keep the device awake the first time so Electrum syncs without being backgrounded.

## 5. Sanity Checklist
- Switch network selector (Mainnet → Signet) and confirm presets change.
- Import a testnet descriptor pair, sync, and confirm balance/UTXO lists populate.
- Exercise the settings toggles (Transaction/UTXO/Wallet health) to ensure analytics run locally.

## 6. IDE Tips
- After dependency updates (`gradle/libs.versions.toml`), run “Sync Project with Gradle Files” inside Android Studio.
- Enable Kotlin official code style: `Settings → Editor → Code Style → Kotlin → Set from Predefined Style`.
- Useful plugins: Kotlin, Compose Multiplatform, and Room/SQL helpers.

## 7. Troubleshooting
- **Tor stuck** → ensure you are using an ARM64 image and that host firewalls allow outbound Tor connections. Inspect `adb logcat | grep TorRuntimeManager`.
- **Descriptor rejected** → confirm it is public (tpub/zpub, no `xprv`) and includes `*` or BIP-389 multipath notation.
- **Gradle cache issues** → run `./gradlew --stop && ./gradlew clean` or nuke `.gradle`/`.idea` directories when IDE sync drifts.

With the environment ready, follow the workflow in `CONTRIBUTING.md` (issue-first planning, `feature/<descriptor>` branches, lint/tests, documentation updates) and coordinate with reviewers/testers as needed.
