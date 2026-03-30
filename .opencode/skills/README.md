# Skill Routing For This Project

This repository includes Android implementation skills and wallet protocol skills.
Use this routing to reduce ambiguity and keep work aligned with Connection V2.

## Priority Skills (Connection + Wallet Core)

1. `bdk-electrum-specialist`
2. `electrum-protocol-local`
3. `bitcoin-bips-local`
4. `android-coroutines`
5. `kotlin-concurrency-expert`
6. `bitcoin-privacy-heuristics` for local-first heuristic wording/review work

Use these first whenever changes affect node transport, sync, descriptors, or retry/lifecycle behavior.

Connection V2 alignment:
- Keep Tor-only behavior as the active runtime policy.
- Avoid silent transport downgrade or automatic transport switching.
- Keep transport policy explicit and fail-closed for future non-Tor modes.

## Supporting Android Skills

- Architecture and state: `android-architecture`, `android-viewmodel`, `android-coroutines`, `kotlin-concurrency-expert`
- Compose UI flows: `compose-ui`, `compose-navigation`, `android-accessibility`, `coil-compose`
- Material system: `material3-android` for Material 3 component/token correctness and hierarchy checks
  - Local references mirror: `.opencode/skills/material3-android/references/material-design/`
- Quality and tooling: `android-testing`, `gradle-build-performance`, `android-gradle-logic`

## Copy and explanation skill

- `wallet-copy-clarity`: refine UI strings and explanatory copy with accuracy-first privacy wording.
- Use together with `docs-steward` when behavior/security posture changes and you need both wording quality and docs parity.

## Privacy heuristic review skill

- `bitcoin-privacy-heuristics`: use when adding/reviewing privacy finding ids, copy, confidence wording, or local-first boundaries.
- Pair with `wallet-copy-clarity` for user-facing strings and with `android-testing` for heuristic coverage.

## Skills Usually Not Primary Here

- `android-emulator-skill`: useful for emulator workflows, but this project prioritizes USB physical device testing.
- `xml-to-compose-migration`: only use for legacy XML migrations.
- `android-retrofit`: only use when introducing or modifying Retrofit-based networking.

## Research Agent

- Use `@technical-researcher` to inspect external apps in `context/inspiration/` and extract patterns with evidence and adaptation notes.

## Extra Local Context

- Use `context/bdk/book-of-bdk-master/` as BDK conceptual reference.
- Use `context/bdk/bdk-android-2.3.1-javadoc/` for exact Kotlin API contracts.
- Use `context/electrum/electrum-protocol-master/docs/` for Electrum wire protocol semantics.
