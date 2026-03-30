# AGENTS.md

## Purpose
This file is the operational contract for coding agents in `utxopocket-android`.
Agents MUST preserve the project's privacy-first watch-only guarantees while delivering minimal, test-backed, and reviewable changes.

## Scope and Precedence
- User prompts override this file.
- This file complements `opencode.json` instructions and does not replace them.
- Mandatory instruction sources for all tasks:
  - `CONTRIBUTING.md`
  - `SECURITY.md`
  - `docs/project-setup.md`
- English is the canonical language for docs and source string definitions.

## Project Stack Snapshot
- Android app in Kotlin.
- UI with Jetpack Compose.
- MVVM + Clean Architecture.
- Dependency Injection with Hilt.
- Persistence with Room + SQLCipher, DataStore, EncryptedSharedPreferences, and EncryptedFile.
- Wallet/network stack based on BDK Android + Electrum.
- Runtime networking policy is Tor-only (fail closed).
- Baseline toolchain: Java 21, Android SDK 36.

## Hard Security and Privacy Invariants (Non-Negotiable)
1. Watch-only trust model:
   - MUST NOT add private key, seed phrase, signing, or secret import paths.
   - MUST keep descriptor handling public-only.
2. Tor-only networking:
   - MUST keep wallet sync traffic behind embedded Tor.
   - MUST NOT add silent clearnet fallback when Tor is unavailable.
3. Panic wipe and data remanence:
   - MUST preserve complete wipe semantics across DB, encrypted bundles, DataStore, Tor state, and caches.
   - MUST avoid partial-delete patterns that can leave recoverable artifacts.
4. No telemetry by default:
   - MUST NOT add analytics, crash reporting SDKs, ad SDKs, or automatic data exfiltration.
5. Documentation parity for security posture:
   - MUST NOT ship behavior that conflicts with `README.md` or `SECURITY.md` guarantees.

## Architecture and Implementation Rules
- MUST preserve clear layering: UI -> ViewModel/state orchestration -> domain/use-cases -> data/repositories.
- MUST maintain unidirectional data flow.
- ViewModel rules:
  - Persistent UI state via `StateFlow`.
  - One-off events via `SharedFlow` with `replay = 0`.
- Coroutines rules:
  - MUST NOT use `GlobalScope`.
  - SHOULD inject dispatchers in data/domain classes for testability.
  - MUST rethrow `CancellationException` when handling generic exceptions.
- Compose rules:
  - Hoist state where practical.
  - Keep composables focused and stable to avoid unnecessary recomposition.
- UI strings MUST live in resources, not inline literals.

## OpenCode Skills and Agent Routing
Use the smallest valid set of skills/agents for the task.

Priority skills for wallet/network core work:
- `bdk-electrum-specialist`
- `electrum-protocol-local`
- `bitcoin-bips-local`
- `bitcoin-privacy-heuristics` (privacy finding certainty boundaries/local-first constraints)
- `android-coroutines`
- `kotlin-concurrency-expert`

Supporting Android skills:
- Architecture and state: `android-architecture`, `android-viewmodel`
- Compose and UX: `compose-ui`, `compose-navigation`, `android-accessibility`, `coil-compose`
- Quality and build logic: `android-testing`, `gradle-build-performance`, `android-gradle-logic`
- Copy precision: `wallet-copy-clarity`

Typical agent routing:
- Feature implementation: `android-feature-engineer`
- Privacy/networking reviews: `privacy-auditor`
- Electrum/BDK protocol checks: `bdk-electrum-engineer`
- Documentation impact and sync: `docs-steward`
- Device build/install/test execution: `android-device-runner`

## Verification Gates
Gradle wrapper execution note for agent sessions:
- In this repository environment, run Gradle via `bash ./gradlew ...` (not bare `./gradlew ...`) to avoid permission issues in automated sessions.

Before claiming completion for code changes, agents MUST run:
1. `bash ./gradlew lintDebug`
2. `bash ./gradlew :app:testDebugUnitTest`

If a required command cannot run, agents MUST report what was skipped and why.

## Documentation and Localization Sync
When behavior, security posture, or user-facing copy changes, agents MUST update relevant docs in the same change set.

Canonical docs to keep aligned:
- Root docs: `README.md`, `SECURITY.md`, `CONTRIBUTING.md`, `docs/project-setup.md`, `.github/RELEASE_TEMPLATE.md`
- User docs: `docs/getting-started.md`, `docs/guides/**`, `docs/wiki/**`, `docs/glossary/**`
- Internal docs: `knowledge/**`

String localization workflow:
- Update English source first: `app/src/main/res/values/strings.xml`
- Then mirror localized entries while preserving placeholders and formatting tokens.

## Git Hygiene
- MUST NOT run destructive git operations unless explicitly requested.
- MUST NOT revert unrelated working tree changes.
- SHOULD keep diffs focused and avoid opportunistic refactors.
- Commit messages MUST follow Conventional Commits: `type(scope): description`.

## When to Ask Before Acting
Agents MUST ask one targeted question when:
- A change impacts security/privacy posture.
- An action is destructive or irreversible.
- Requirements are ambiguous in a way that materially changes behavior.
- Secrets/credentials are required and unavailable.

When asking, provide a recommended default and describe what changes with alternatives.

## Definition of Done
A task is complete only when all conditions are met:
- Security/privacy invariants are preserved.
- Architecture boundaries remain consistent.
- Required verification commands were run (or limitations explicitly documented).
- Relevant documentation is synchronized with the implemented behavior.
- Final report includes changed files, verification status, and residual risks.
