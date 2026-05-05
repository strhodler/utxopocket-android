# Project Setup For Contributors

Follow these steps to run UtxoPocket from source, reproduce the CI checks, and install debug builds on devices.

## 1. Requirements
- Linux, macOS, or Windows Subsystem for Linux.
- Git, `adb`, and Java **21** (Temurin/OpenJDK). `java -version` should report 21.x.
- Android Studio Koala (or newer) with:
  - Android SDK Platform **37** + build tools (matches `compileSdk`; `targetSdk` remains 36)
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

## 4. Install The Debug Build
```bash
adb devices             # ensure your target shows “device”
./gradlew :app:installDebug
```
Launch the “UtxoPocket” icon, complete onboarding, and verify initial connectivity (Tor bootstrap in default mode, or Local Direct only if explicitly selected). Keep the device awake the first time so Electrum syncs without being backgrounded.

### 4.1 WSL + USB Devices
On WSL, Gradle's `:app:installDebug` may not detect USB devices even when `adb.exe` from Windows does. Use this fallback flow:

```bash
./gradlew :app:assembleDebug
"/mnt/c/Users/<windows-user>/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/debug/app-debug.apk"
"/mnt/c/Users/<windows-user>/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.strhodler.utxopocket/.presentation.MainActivity
```

If your Android SDK lives in another Windows profile, replace `<windows-user>` in the path.

## 5. Sanity Checklist
- Switch network selector (Mainnet → Signet) and confirm presets change.
- Import a testnet descriptor pair, sync, and confirm balance/UTXO lists populate.
- Trigger a full rescan from wallet detail after import and confirm the sync indicator completes.
- If testing Local Direct, use a custom private/local IP literal endpoint only (no DNS/`.local`/hostnames), and verify custom-node save is blocked when Electrum genesis hash does not match the selected network.

## 6. IDE Tips
- After dependency updates (`gradle/libs.versions.toml`), run “Sync Project with Gradle Files” inside Android Studio.
- Enable Kotlin official code style: `Settings → Editor → Code Style → Kotlin → Set from Predefined Style`.
- Useful plugins: Kotlin, Compose Multiplatform, and Room/SQL helpers.

## 7. Troubleshooting
- **Tor stuck** → ensure you are using an ARM64 image and that host firewalls allow outbound Tor connections. Inspect `adb logcat | grep TorRuntimeManager`.
- **Local Direct endpoint rejected** → use only private/local IP literals (IPv4/IPv6), avoid DNS/`.local`/hostnames, and confirm the node network matches the selected app network.
- **Descriptor rejected** → confirm it is public (tpub/zpub, no `xprv`) and includes `*` or BIP-389 multipath notation.
- **Gradle cache issues** → run `./gradlew --stop && ./gradlew clean` or nuke `.gradle`/`.idea` directories when IDE sync drifts.
- **No connected devices (WSL)** → if Linux `adb devices` is empty but `adb.exe devices` shows your phone, use the fallback commands from section **4.1 WSL + USB Devices**.
- **Toolchain mismatch** → this project requires Java 21. If `java -version` is not 21.x, set `JAVA_HOME` to a JDK 21 installation.

With the environment ready, follow the workflow in `CONTRIBUTING.md` (issue-first planning, `feature/<descriptor>` branches, lint/tests, documentation updates) and coordinate with reviewers/testers as needed.
