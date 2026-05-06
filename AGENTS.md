# AGENTS.md

This file is the root agent guide for UtxoPocket. Follow it together with the closest nested `AGENTS.md` if one is added later; explicit user instructions still take precedence.

## Project Overview

UtxoPocket is a privacy-first, open-source Android watch-only wallet for monitoring Bitcoin descriptors, analyzing UTXOs, and inspecting transaction flows. It never handles seeds, private keys, or transaction signing.

Key stack:

- Single Android app module: `:app`.
- Kotlin, Java 21 toolchain, Gradle Kotlin DSL, Android Gradle Plugin via `gradle/libs.versions.toml`.
- Jetpack Compose with Material 3, Navigation Compose, Lifecycle ViewModel, Paging.
- Hilt for dependency injection.
- Coroutines and Flow for async state and pipelines.
- Room with SQLCipher, Preferences DataStore, and Google Tink for local persistence and encryption.
- BDK Android for descriptor parsing, watch-only wallet state, wallet persistence, and Electrum sync.
- Guardian Project `tor-android` plus pinned `jtorctl` for embedded Tor runtime.

There is no JavaScript package manager or app development server. Build, run, lint, and test through the Gradle wrapper and Android tooling.

Core product invariants:

- Watch-only only: do not introduce seeds, mnemonics, xprv/tprv, WIF, private descriptors, PSBT signing, transaction construction, or signing/finalization flows.
- Tor is the default privacy boundary for bundled public Electrum presets and custom onion endpoints.
- Local Direct is explicit opt-in only for custom private/local IP literal Electrum endpoints.
- Never add silent fallback between Tor and Local Direct, and never add clearnet fallback while Tor mode is active.
- Keep all network, Tor, endpoint, descriptor, backup, PIN, duress, SQLCipher, Tink, and panic-wipe behavior aligned with `README.md`, `SECURITY.md`, and `CONTRIBUTING.md`.

## Repository Layout

- `app/src/main/java/com/strhodler/utxopocket/common`: shared encoding and secure logging utilities.
- `app/src/main/java/com/strhodler/utxopocket/domain`: domain models, repository interfaces, and service contracts.
- `app/src/main/java/com/strhodler/utxopocket/data`: infrastructure implementations for BDK, Electrum, Room, DataStore, security, Tor, wallet sync, logs, wiki, glossary, and UTXO canvas.
- `app/src/main/java/com/strhodler/utxopocket/di`: Hilt modules and coroutine dispatcher qualifiers.
- `app/src/main/java/com/strhodler/utxopocket/presentation`: Compose UI, navigation, ViewModels, theme, settings, wallets, wiki, node, PIN, onboarding, and app shell.
- `app/src/main/java/com/strhodler/utxopocket/tor`: embedded Tor foreground service, runtime manager, control facade helpers, notifications, and sanitization.
- `app/src/main/res/values/strings.xml`: canonical English UI strings.
- `app/src/main/res/values-es/strings.xml`: Spanish translations that must stay key-compatible with English.
- `docs/wiki` and `docs/glossary`: Markdown content copied into generated runtime assets by Gradle.
- `docs/contributing`: wiki and glossary authoring rules.
- `context`: local technical references for BDK, Bitcoin BIPs, Electrum, and Tor when present in the checkout.
- `.agents/skills`: project-local agent skills when present in the checkout.

## Setup Commands

- Install JDK 21; the Gradle daemon is pinned to Java 21 through `gradle/gradle-daemon-jvm.properties`, and `java -version` reporting 21.x is recommended for local CLI consistency.
- Install Android Studio with Android SDK Platform 37, build tools, and Android NDK 26+.
- Use an ARM64 Android device or ARM64 emulator for runtime testing because bundled Tor binaries are not x86-focused.
- Create `local.properties` locally and do not commit it: `sdk.dir=/absolute/path/to/Android/Sdk`.
- On Windows PowerShell, use `./gradlew.bat` or `.\gradlew.bat`; on Linux, macOS, or WSL, use `./gradlew`.
- Sync dependencies through the Gradle wrapper, not through an external package manager.

Useful setup checks:

```bash
./gradlew --version
./gradlew :app:tasks
```

## Development Workflow

- Build debug APK: `./gradlew :app:assembleDebug`.
- Install debug APK on a connected device: `./gradlew :app:installDebug`.
- Run the fast contributor checks before handing off code: `./gradlew lintDebug` and `./gradlew :app:testDebugUnitTest`.
- After dependency version changes in `gradle/libs.versions.toml`, update dependency verification metadata with `./gradlew --write-verification-metadata sha256 :app:assembleDebug :app:testDebugUnitTest` and commit `gradle/verification-metadata.xml` with the version change.
- If Gradle or Android Studio sync drifts, run `./gradlew --stop` and then retry the exact task before using broader cleanup.
- Generated docs assets are built from `docs/wiki` and `docs/glossary` through the `syncRuntimeDocsAssets` Gradle task; edit source Markdown, not generated files under `build/`.
- Android instrumented tests are disabled for all variants in `app/build.gradle.kts`; prefer JVM unit tests unless the build configuration changes.
- CI is defined in `.github/workflows/android-checks.yml` and runs `./gradlew lintDebug :app:testDebugUnitTest` on pushes to `main`/`release/**` and on pull requests.
- Do not commit generated outputs such as `build/`, `app/build/`, APKs, AABs, local SDK files, keystores, logs, or machine-specific IDE files.

WSL connected-device fallback:

```bash
./gradlew :app:assembleDebug
"/mnt/c/Users/<windows-user>/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/debug/app-debug.apk"
"/mnt/c/Users/<windows-user>/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.strhodler.utxopocket/.presentation.MainActivity
```

## Testing Instructions

- Run lint with warnings as errors: `./gradlew lintDebug`.
- Run all JVM unit tests: `./gradlew :app:testDebugUnitTest`.
- Run one test class: `./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.data.wallet.sync.WalletSyncEngineTest"`.
- Run one test method: `./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.data.wallet.sync.WalletSyncEngineTest.fullScanDecisionPreservesExistingTriggerSemantics"`.
- Unit tests live under `app/src/test/java` and use JUnit4, `kotlin.test`, `kotlinx-coroutines-test`, Robolectric, AndroidX Test Core, and Room testing.
- Add or update focused unit tests for every behavior change, especially descriptor validation, BDK persistence, sync decisions, Tor fail-closed policy, Local Direct validation, SQLCipher/Tink behavior, panic wipe, backup import/export, PIN/duress, Room queries, reducers, and ViewModels.
- Keep coroutine tests deterministic with `runTest`, `StandardTestDispatcher`, `Dispatchers.setMain`, and `Dispatchers.resetMain` when testing ViewModels or main-dispatcher behavior.
- Always rethrow `CancellationException`; add regression tests when touching coroutine failure handling.
- There is no configured coverage task in this repo; do not claim coverage results unless a task is added and run.

## Code Style

- Follow Kotlin official style from `gradle.properties` and JetBrains defaults.
- Keep changes small, cohesive, and behavior-preserving unless the task explicitly changes behavior.
- Prefer existing project boundaries over new abstractions; do not introduce speculative helpers, compatibility layers, service locators, or framework wrappers without a concrete need.
- Avoid technical debt by removing dead code, naming concepts consistently, limiting mutable state, preserving tests, and updating docs in the same change as user-facing behavior.
- Keep domain contracts in `domain`, infrastructure in `data`, DI wiring in `di`, and UI/presentation state in `presentation`.
- Domain should not depend on Android framework, BDK native objects, Room entities, Compose types, or Hilt annotations.
- Keep raw BDK API usage inside `data/bdk` and wallet data-layer wrappers unless a new boundary is explicitly justified and tested.
- Use Hilt constructor injection and existing modules in `di`; prefer dispatcher qualifiers from `CoroutinesModule` over hard-coded dispatchers in production code.
- Use Flow and immutable UI state for presentation models; expose read-only `StateFlow` or `SharedFlow` where possible.
- Compose UI should be stateless where practical, route events upward, use Material 3 components/tokens, and avoid inline user-visible strings.
- Put user-visible strings in `app/src/main/res/values/strings.xml` first, then mirror localized keys in `values-es/strings.xml` while preserving placeholders and formatting.
- Use succinct comments only for non-obvious security, lifecycle, concurrency, or protocol reasoning.

## Security And Privacy Rules

- Do not add analytics, crash reporters, ad SDKs, telemetry, remote attribution services, or explorer/entity lookup calls.
- Do not log raw descriptors, raw endpoint host:port values, onion hosts, local IPs, SOCKS ports, PIN data, passphrases, backup contents, SQLCipher keys, Tink keysets, or BDK storage paths.
- Use existing secure logging and sanitization helpers for Tor/network text and user-copyable diagnostics.
- Treat descriptor parsing, BIP-389 multipath handling, BIP-329 labels, derivation paths, Taproot, address formats, and wallet backup JSON as security-sensitive and interoperability-sensitive.
- Preserve explicit private-material rejection even if BDK parsing succeeds.
- BDK wallet persistence errors, persister lifecycle failures, sealing failures, and SQLCipher/Tink initialization failures must surface as failures; do not silently delete or recreate wallet storage.
- Panic wipe must keep cancelling active work, clearing Room/DataStore/Tink/BDK/Tor/cache artifacts, and restarting into onboarding without leaving recoverable wallet metadata.
- Tor-required Electrum work must wait or fail closed if the Tor proxy is unavailable.
- Local Direct must reject DNS names, `.local`, public IPs, public presets, onion endpoints, and automatic mode switches.
- Incoming transaction detection is lightweight and non-canonical; BDK sync remains the canonical wallet state.
- Privacy heuristic wording must distinguish deterministic facts from probabilistic inferences and must stay local-first.

## BDK And Electrum Guidelines

- Verify the active BDK version in `gradle/libs.versions.toml` before changing BDK code.
- Prefer `BdkWalletFactory`, `WalletStorage`, `BdkPersisterRegistry`, `ElectrumBlockchain`, `ElectrumEndpointProvider`, `ElectrumSessionCoordinator`, and wallet repository managers over direct BDK usage.
- Keep descriptor validation and wallet factory behavior aligned for public-only, receive/change, multipath branch count, wildcard, checksum, and network rules.
- Full scan is for bootstrap/discovery, explicit rescan, or tested fresh/corrupt local-state recovery; regular refresh should use incremental sync when persisted state is coherent.
- Do not lower stop-gap or increase batch sizes without tests and a privacy/performance review because these choices affect missed funds, Tor reliability, and server load.
- Raw Electrum clients must send `server.version` first, use standard scripthash APIs, validate custom-node genesis hash, and respect active transport policy.
- For scripthash code, derive from ScriptPubKey bytes, compute SHA256, reverse the digest, and lowercase hex encode.

## Documentation Rules

- Use `documentation-writer` for substantial README, guide, wiki, glossary, or contributor-doc work; it follows Diataxis and should keep audience, task, scope, and document type explicit.
- Keep `README.md`, `SECURITY.md`, `CONTRIBUTING.md`, `docs/project-setup.md`, and `.github/RELEASE_TEMPLATE.md` aligned with behavior changes.
- Update `docs/getting-started.md`, `docs/guides`, `docs/wiki`, and `docs/glossary` for user-visible feature or terminology changes.
- Wiki and glossary content must be English, layer-1 Bitcoin only, watch-only aware, and follow `docs/contributing/authoring-wiki-and-glossary.md`.
- Wiki files in `docs/wiki` require valid frontmatter with IDs, title, summary, category fields, related IDs, glossary refs, and keywords.
- Glossary files in `docs/glossary` require valid frontmatter with ID, title, summary, related IDs, and keywords.
- Do not use changelog wording in canonical docs; document current behavior and guarantees.

## Local Agent Assets

- Project-local skills may be present under `.agents/skills`; use only skills that exist in this repository checkout.
- There are no project-local agents configured under `.agents/agents` in the current repository state; do not assume a docs, review, or command agent exists unless the file is added locally.
- Use `android-architecture-clean` for Clean Architecture boundaries and lifecycle-aware presentation models.
- Use `android-compose-foundations` and `material3-android` for Compose and Material 3 UI work.
- Use `android-coroutines-flow` for coroutine, Flow, dispatcher, and cancellation-sensitive work.
- Use `android-di-hilt` for Hilt modules, scopes, and testing overrides.
- Use `android-gradle-build-logic` and `gradle-build-performance` for Gradle, version catalog, toolchain, or build performance work.
- Use `android-kotlin-core` for Kotlin idioms, nullability, sealed types, and collection pipelines.
- Use `android-networking-retrofit-okhttp` for Retrofit/OkHttp networking changes if such a stack is introduced or encountered.
- Use `android-testing-unit` for focused JVM unit tests.
- Use `bdk-specialist` for BDK Android, descriptors, BIP-389 multipath, wallet/persister lifecycle, Electrum sync through BDK, and SQLite persistence.
- Use `bitcoin-bips-local` for descriptor, address, label, Taproot, PSBT-inspection, and BIP semantics work.
- Use `bitcoin-privacy-heuristics` for privacy findings, heuristic wording, and local-first analysis boundaries.
- Use `create-agentsmd` when creating or materially revising this file.
- Use `create-readme` when creating or materially revising README-style project documentation.
- Use `documentation-writer` for Diataxis-oriented tutorials, how-to guides, references, explanations, and larger docs rewrites.
- Use `electrum-protocol-local` for raw Electrum protocol, scripthash lookup, server features, endpoint validation, and incoming transaction detection.
- Use `git-commit` only when the user explicitly asks to create a commit or invokes `/commit`.
- Use `java-coding-standards` and `java-docs` only for Java source or Javadoc work.
- Use `refactor` for surgical behavior-preserving cleanups.
- Use `solid` when designing or reviewing larger architecture, feature, or refactor changes.
- Use `tor-android-specialist` for embedded Tor runtime, SOCKS routing, Tor control, NEWNYM, fail-closed policy, and Tor tests.
- Use only repository-local skills, agents, and commands that are present in this checkout.

## Build And Release

- Debug build output: `app/build/outputs/apk/debug/app-debug.apk`.
- Release builds enable minification, resource shrinking, JNI legacy packaging, and ProGuard keep rules for BDK, Tor, SQLCipher, and JNA.
- Release metadata and verification wording should follow `.github/RELEASE_TEMPLATE.md`.
- Do not claim CI behavior unless repository workflows are present, tracked, and verified.
- Before release or PR handoff, run `./gradlew lintDebug` and `./gradlew :app:testDebugUnitTest` and paste exact outputs or failures in the handoff.
- Never commit release keystores, signing credentials, local properties, APK/AAB artifacts, or private disclosure material.

## Pull Request Guidelines

- Follow `CONTRIBUTING.md`: discuss significant changes first, branch from `main` using `feature/<descriptor>`, and use Conventional Commits such as `feat(wallets): add testnet selector`.
- Keep each PR focused on one logical change and avoid mixing refactors with feature or security behavior changes unless required to make the feature safe.
- Include issue reference, user-facing summary, exact Gradle commands run, manual test steps, docs updated, and confirmation that no private keys or telemetry were introduced.
- For UI changes, include screenshots or concise manual verification notes for relevant screens.
- For security-sensitive changes, explicitly mention Tor mode, Local Direct, panic wipe, descriptor import, backup, PIN/duress, SQLCipher/Tink, or BDK persistence checks as applicable.

## Troubleshooting

- Tor stuck: verify ARM64 runtime and inspect sanitized Tor logs; do not bypass Tor to make sync succeed.
- Local Direct rejected: use only custom private/local IP literal endpoints with `tcp://`; do not use DNS names, `.local`, hostnames, public IPs, onion addresses, or public presets.
- Descriptor rejected: confirm it is public, network-compatible, includes wildcard derivation where needed, and has receive/change coverage via multipath or paired descriptors.
- Gradle toolchain mismatch: ensure Java 21 is active and Android SDK Platform 37 is installed.
- WSL device not found: use Windows `adb.exe` fallback from `docs/project-setup.md`.
- Dependency verification failed: if dependency versions changed, regenerate trusted hashes with `./gradlew --write-verification-metadata sha256 :app:assembleDebug :app:testDebugUnitTest`; otherwise inspect the verification report before trusting new artifacts.
- Room schema or migration failures: update database version, migrations, DAO tests, and schema expectations together; do not drop user data unless there is an explicit, tested migration/recovery path.
- Flaky coroutine tests: use test dispatchers, collect flows deliberately, call `advanceUntilIdle`, and reset `Dispatchers.Main` in teardown.
