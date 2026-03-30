# Wallet Privacy Heuristics Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Workspace constraint:** Do not use git worktrees for this plan. Execute from the current workspace unless the user explicitly asks for a different isolation strategy.

**Goal:** Add a local-first Bitcoin privacy heuristics engine to UtxoPocket so wallet analysis, transaction detail, and UTXO detail can surface actionable privacy findings using only already-synced wallet data.

**Architecture:** Build a pure Kotlin domain privacy layer with analyzers per scope (`wallet`, `transaction`, `utxo`), a common finding model, and a small cross-heuristic rule engine for suppressions, escalations, and deduplication. Analyzers run in the ViewModel/domain orchestration path from already-available screen snapshots only; reducers and composables remain presentation-only, receiving precomputed summaries and findings. Presentation resolves copy from stable finding ids into Android string resources and renders the findings inside existing Material 3 screens without changing sync, persistence, or networking behavior.

**Tech Stack:** Kotlin, Hilt constructor injection, StateFlow, Jetpack Compose, Android string resources, JUnit unit tests, existing wallet detail reducers/viewmodels.

---

## Scope and Guardrails

- Local-first only for v1: no new network calls, no ancestry tracing, no entity lookups, no mempool/explorer augmentation.
- Preserve watch-only and Tor-only guarantees: this work must not change wallet sync, transport, or descriptor handling.
- Findings must distinguish deterministic facts from probabilistic inferences. Do not present CIOH-style or change-detection-style findings as certainty.
- Do not add a public numeric privacy score in v1. Surface severity, confidence, evidence, and next actions first.
- Do not persist findings in Room or DataStore in v1. Recompute from current UI/domain state.
- Analyzers must consume only already-loaded in-memory screen/domain data (`WalletDetail`, transaction/UTXO detail state, and canvas snapshot). No new repository methods, background jobs, persistence, caches, or network lookups in v1.
- Leave an explicit seam for future opt-in deep analysis, but keep it inactive and undocumented as a runtime feature.

## Reference Takeaways From `context/am-i-exposed-main`

- Reuse the good parts:
  - registry/orchestrator pattern
  - wallet audit as a separate analyzer
  - cross-heuristic suppressions and escalations
  - precise heuristic ids and evidence-backed findings
- Do not import into v1:
  - multi-hop chain tracing
  - entity databases and bloom filters
  - Boltzmann/entropy computation
  - public 0-100 grade model
  - network-heavy address/xpub scans beyond what UtxoPocket already syncs

## Initial Heuristic Catalog For V1

### Wallet-level

- Address reuse exposure
- Dust exposure and toxic-change risk
- Fragmentation and future consolidation pressure
- Change-heavy balance concentration
- Mixed script/address family exposure across owned UTXOs
- Label/collection hygiene gaps
- Positive signals: low reuse, low dust, labeled/organized set, changeless-spend history where detectable

### Transaction-level

- Multi-input ownership exposure (cautious wording)
- Consolidation/self-transfer patterns
- Probable change exposure
- Exact/changeless spend as positive signal
- Mixed script family fingerprints
- Fee and transaction-structure hints from already stored metadata
- CoinJoin-like equal-output pattern as informational/positive signal with suppressions

### UTXO-level

- Reused receive address exposure
- Change-origin UTXO context
- Dust or near-dust warning
- Unlabeled/unassigned organizational risk
- Spendability and maturity context as informational finding

## File Map

### Create

- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/PrivacyModels.kt`
- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/PrivacySummary.kt`
- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/PrivacyAugmentedContext.kt`
- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRules.kt`
- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/WalletPrivacyAnalyzer.kt`
- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/TransactionPrivacyAnalyzer.kt`
- `app/src/main/java/com/strhodler/utxopocket/domain/privacy/UtxoPrivacyAnalyzer.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/PrivacyFindingUiText.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/PrivacyFindingCard.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletPrivacySummarySection.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletPrivacyFindingsSection.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionPrivacySection.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoPrivacySection.kt`
- `app/src/test/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRulesTest.kt`
- `app/src/test/java/com/strhodler/utxopocket/domain/privacy/WalletPrivacyAnalyzerTest.kt`
- `app/src/test/java/com/strhodler/utxopocket/domain/privacy/TransactionPrivacyAnalyzerTest.kt`
- `app/src/test/java/com/strhodler/utxopocket/domain/privacy/UtxoPrivacyAnalyzerTest.kt`
- `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailUiReducerPrivacyTest.kt`
- `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailViewModelPrivacyTest.kt`
- `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailViewModelPrivacyTest.kt`
- `knowledge/features/privacy-heuristics.md`
- `.opencode/skills/bitcoin-privacy-heuristics/SKILL.md`
- `.opencode/skills/bitcoin-privacy-heuristics/references/heuristic-catalog.md`
- `.opencode/skills/bitcoin-privacy-heuristics/references/local-first-boundary.md`

### Modify

- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailViewModel.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailUiReducer.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoVisualizerRoute.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionUtxoDetailUiState.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailViewModel.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailScreen.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailViewModel.kt`
- `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-es/strings.xml`
- `README.md`
- `docs/wiki/wallet-analysis.md`
- `docs/wiki/bitcoin-privacy.md`
- `.opencode/skills/README.md`
- `AGENTS.md`

---

## Chunk 1: Domain Foundation

### Task 1: Create the privacy finding model and summary contracts

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/PrivacyModels.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/PrivacySummary.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/PrivacyAugmentedContext.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRulesTest.kt`

- [x] **Step 1: Define common finding enums and models**

```kotlin
enum class PrivacySeverity { Info, Positive, Caution, Warning, Critical }
enum class PrivacyConfidence { Deterministic, High, Medium, Low }
enum class PrivacyScope { Wallet, Transaction, Utxo }

data class PrivacyFinding(
    val id: String,
    val scope: PrivacyScope,
    val severity: PrivacySeverity,
    val confidence: PrivacyConfidence,
    val evidence: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap()
)
```

- [x] **Step 2: Add summary models that do not require strings**
- [x] **Step 3: Add `PrivacyAugmentedContext` as the future deep-analysis seam**
- [x] **Step 4: Keep this layer pure Kotlin and free of Android resource access**
- [x] **Step 5: Write the first test file with model-level invariants and summary ordering**
- [x] **Step 6: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.domain.privacy.CrossHeuristicRulesTest"`

Expected: the new domain privacy test target passes.

- [x] **Step 7: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/domain/privacy app/src/test/java/com/strhodler/utxopocket/domain/privacy
git commit -m "feat(analysis): add privacy finding domain contracts"
```

### Task 2: Implement cross-heuristic suppression and deduplication rules

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRules.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRulesTest.kt`

- [x] **Step 1: Add a single cross-rule entry point**

```kotlin
class CrossHeuristicRules @Inject constructor() {
    fun apply(findings: List<PrivacyFinding>): List<PrivacyFinding> = findings
}
```

- [x] **Step 2: Implement minimum v1 rules**
  - suppress probable-change and multi-input-ownership penalties when a transaction looks coinjoin-like
  - suppress redundant change findings when a changeless/self-transfer finding already explains the structure
  - deduplicate overlapping dust findings between wallet and UTXO scopes
  - escalate severity when multiple independent clues point to the same privacy risk
- [x] **Step 3: Add tests for each suppression/escalation path**
- [x] **Step 4: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.domain.privacy.CrossHeuristicRulesTest"`

Expected: all suppression and deduplication cases pass.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRules.kt app/src/test/java/com/strhodler/utxopocket/domain/privacy/CrossHeuristicRulesTest.kt
git commit -m "feat(analysis): add privacy cross-heuristic rules"
```

---

## Chunk 2: Wallet Analysis Surface

### Task 3: Build the wallet privacy analyzer

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/WalletPrivacyAnalyzer.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/privacy/WalletPrivacyAnalyzerTest.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/domain/model/WalletModels.kt` (only if a small helper extension is needed; otherwise avoid touching it)

- [x] **Step 1: Start with a failing test per heuristic family**
  - reused address exposure
  - dust/toxic change exposure
  - consolidation pressure from fragmented UTXO set
  - missing labels/collections organizational risk
  - mixed script family exposure
  - positive hygiene cases
- [x] **Step 2: Implement `WalletPrivacyAnalyzer` against `WalletDetail`, `WalletUtxo`, and `WalletTransaction` only**
- [x] **Step 3: Feed raw findings through `CrossHeuristicRules` before returning**
- [x] **Step 4: Keep IDs stable and descriptive (`wallet-address-reuse`, `wallet-dust-pressure`, etc.)**
- [x] **Step 5: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.domain.privacy.WalletPrivacyAnalyzerTest"`

Expected: wallet analyzer tests pass without requiring any new repository or network dependency.

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/domain/privacy/WalletPrivacyAnalyzer.kt app/src/test/java/com/strhodler/utxopocket/domain/privacy/WalletPrivacyAnalyzerTest.kt
git commit -m "feat(wallets): add wallet privacy analyzer"
```

### Task 4: Integrate wallet findings into the reducer and Analysis route

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailViewModel.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailUiReducer.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoVisualizerRoute.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/PrivacyFindingUiText.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/PrivacyFindingCard.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletPrivacySummarySection.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletPrivacyFindingsSection.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailUiReducerPrivacyTest.kt`

- [x] **Step 1: Extend `WalletDetailUiState` with wallet privacy summary + findings**
- [x] **Step 2: Inject `WalletPrivacyAnalyzer` into `WalletDetailViewModel` only using constructor injection**
- [x] **Step 3: Compute findings from the already-available `WalletDetail` snapshot only when wallet detail changes; do not tie recomputation to chart/filter/treemap UI state**
- [x] **Step 4: Add a privacy summary block above the existing donut/treemap analysis in `UtxoVisualizerRoute.kt`**
- [x] **Step 5: Preserve the current charts and interactions; the new section must be additive**
- [x] **Step 6: Resolve all copy from string resources using finding ids and params**
- [x] **Step 7: Add reducer tests for loading, empty, and populated privacy states**
- [x] **Step 8: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailUiReducerPrivacyTest"`

Expected: reducer and privacy summary wiring pass without breaking existing Analysis behavior.

- [x] **Step 9: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/WalletDetailUiReducerPrivacyTest.kt
git commit -m "feat(wallets): surface wallet privacy findings in analysis"
```

---

## Chunk 3: Transaction and UTXO Detail Surfaces

### Task 5: Build the transaction privacy analyzer

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/TransactionPrivacyAnalyzer.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/privacy/TransactionPrivacyAnalyzerTest.kt`

- [x] **Step 1: Write failing tests around stored transaction metadata only**
  - multi-input ownership exposure with low/medium confidence
  - self-transfer and consolidation fan-in
  - probable change exposure
  - exact/changeless spend positive signal
  - mixed script family fingerprint
  - coinjoin-like pattern triggering suppressions
- [x] **Step 2: Implement the analyzer over `WalletTransaction`, `WalletTransactionInput`, and `WalletTransactionOutput`**
- [x] **Step 3: Reuse `CrossHeuristicRules` to prevent double-counting**
- [x] **Step 4: Prefer conservative wording whenever inputs/outputs are ambiguous**
- [x] **Step 5: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.domain.privacy.TransactionPrivacyAnalyzerTest"`

Expected: transaction analyzer tests pass for both negative and positive signals.

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/domain/privacy/TransactionPrivacyAnalyzer.kt app/src/test/java/com/strhodler/utxopocket/domain/privacy/TransactionPrivacyAnalyzerTest.kt
git commit -m "feat(transactions): add transaction privacy analyzer"
```

### Task 6: Surface transaction findings in transaction detail

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionUtxoDetailUiState.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailViewModel.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailScreen.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionPrivacySection.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailViewModelPrivacyTest.kt`

- [x] **Step 1: Add privacy findings and summary fields to `TransactionDetailUiState`**
- [x] **Step 2: Inject `TransactionPrivacyAnalyzer` into `TransactionDetailViewModel`**
- [x] **Step 3: Compute findings only when the transaction is available**
- [x] **Step 4: Add a dedicated section in `TransactionDetailScreen.kt` after overview/status and before raw-hex/explorer actions**
- [x] **Step 5: Reuse `PrivacyFindingCard` to keep UI consistent**
- [x] **Step 6: Add ViewModel tests for not-found, empty-findings, and populated-findings cases**
- [x] **Step 7: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.presentation.wallets.detail.TransactionDetailViewModelPrivacyTest"`

Expected: privacy findings appear in transaction detail without changing existing label or explorer behavior.

- [x] **Step 8: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/Transaction* app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionDetailViewModelPrivacyTest.kt
git commit -m "feat(transactions): show privacy findings in transaction detail"
```

### Task 7: Build the UTXO privacy analyzer

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/privacy/UtxoPrivacyAnalyzer.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/privacy/UtxoPrivacyAnalyzerTest.kt`

- [x] **Step 1: Write failing tests for UTXO-specific risk cases**
  - reused receive address
  - dust or near-dust output
  - change output context
  - unlabeled/unassigned organizational risk
  - non-spendable informational context
- [x] **Step 2: Implement `UtxoPrivacyAnalyzer` using only `WalletUtxo`, related transaction context, and collection membership already available in the detail screen flows**
- [x] **Step 3: Keep findings short and action-oriented; UTXO detail needs compact guidance**
- [x] **Step 4: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.domain.privacy.UtxoPrivacyAnalyzerTest"`

Expected: UTXO analyzer tests pass with no repository expansion.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/domain/privacy/UtxoPrivacyAnalyzer.kt app/src/test/java/com/strhodler/utxopocket/domain/privacy/UtxoPrivacyAnalyzerTest.kt
git commit -m "feat(utxos): add utxo privacy analyzer"
```

### Task 8: Surface UTXO findings in UTXO detail

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/TransactionUtxoDetailUiState.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailViewModel.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailScreen.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoPrivacySection.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailViewModelPrivacyTest.kt`

- [x] **Step 1: Add privacy fields to `UtxoDetailUiState`**
- [x] **Step 2: Inject `UtxoPrivacyAnalyzer` into `UtxoDetailViewModel`**
- [x] **Step 3: Pull assigned collection context from the existing canvas snapshot flow**
- [x] **Step 4: Add a compact privacy section to `UtxoDetailScreen.kt` near metadata/labeling controls**
- [x] **Step 5: Add ViewModel tests for not-found and populated UTXO privacy states**
- [x] **Step 6: Run the focused test command**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.presentation.wallets.detail.UtxoDetailViewModelPrivacyTest"`

Expected: UTXO detail shows contextual privacy findings without disrupting label, collection, or spendable toggles.

- [x] **Step 7: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/Utxo* app/src/test/java/com/strhodler/utxopocket/presentation/wallets/detail/UtxoDetailViewModelPrivacyTest.kt
git commit -m "feat(utxos): show privacy findings in utxo detail"
```

---

## Chunk 4: Strings, Docs, and Specialist Skill

### Task 9: Add string resources and copy mapping

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/PrivacyFindingUiText.kt`

- [x] **Step 1: Define a stable naming scheme for privacy strings**
  - section titles
  - summary labels
  - finding titles
  - finding explanations
  - next-action strings
- [x] **Step 2: Map each finding id to resource ids in `PrivacyFindingUiText.kt`**
- [x] **Step 3: Keep wording precise and non-sensational**
- [x] **Step 4: Mirror English entries into `values-es/strings.xml` preserving placeholders**
- [x] **Step 5: Spot-check the new copy against `README.md` and `SECURITY.md` guarantees**
- [x] **Step 6: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml app/src/main/java/com/strhodler/utxopocket/presentation/wallets/detail/PrivacyFindingUiText.kt
git commit -m "feat(copy): add privacy findings strings"
```

### Task 10: Document the feature and its local-first boundary

**Files:**
- Modify: `README.md`
- Modify: `docs/wiki/wallet-analysis.md`
- Modify: `docs/wiki/bitcoin-privacy.md`
- Create: `knowledge/features/privacy-heuristics.md`

- [x] **Step 1: Update `README.md` feature bullets to mention wallet privacy heuristics if the shipped UI now exposes them**
- [x] **Step 2: Update `docs/wiki/wallet-analysis.md` to explain the new privacy findings section and its local-only behavior**
- [x] **Step 3: Update `docs/wiki/bitcoin-privacy.md` to frame these findings as review aids, not ground truth**
- [x] **Step 4: Write `knowledge/features/privacy-heuristics.md` with architecture, supported heuristics, and out-of-scope deep-analysis notes**
- [x] **Step 5: Explicitly document future deep-analysis seam and opt-in requirement without exposing it as a current feature**
- [x] **Step 6: Commit**

```bash
git add README.md docs/wiki/wallet-analysis.md docs/wiki/bitcoin-privacy.md knowledge/features/privacy-heuristics.md
git commit -m "docs(analysis): document local-first privacy heuristics"
```

### Task 11: Create the `bitcoin-privacy-heuristics` specialist skill (follow-up, non-blocking for app feature ship)

**Files:**
- Create: `.opencode/skills/bitcoin-privacy-heuristics/SKILL.md`
- Create: `.opencode/skills/bitcoin-privacy-heuristics/references/heuristic-catalog.md`
- Create: `.opencode/skills/bitcoin-privacy-heuristics/references/local-first-boundary.md`
- Modify: `.opencode/skills/README.md`
- Modify: `AGENTS.md`

This task stays in the plan because the feature will benefit from a reusable privacy-review workflow, but it is intentionally non-blocking for shipping the Android app feature itself.

- [x] **Step 1: Follow `writing-skills` discipline before authoring the skill**
  - run baseline scenarios where an agent overstates ownership/change certainty
  - record failures before writing the skill
- [x] **Step 2: Write the skill around the actual failure modes**
  - deterministic vs heuristic claims
  - CoinJoin/PayJoin/multisig exceptions
  - local-first boundary in v1
  - no new network assumptions
  - docs/tests required for each heuristic addition
- [x] **Step 3: Add a compact heuristic catalog in the references folder**
- [x] **Step 4: Add routing hints in `.opencode/skills/README.md` and project-level guidance in `AGENTS.md`**
- [x] **Step 5: Re-test the skill against the same scenarios and tighten loopholes**
- [x] **Step 6: Commit**

```bash
git add .opencode/skills/bitcoin-privacy-heuristics .opencode/skills/README.md AGENTS.md
git commit -m "feat(skills): add bitcoin privacy heuristics specialist skill"
```

---

## Chunk 5: Final Verification and Release Readiness

### Task 12: Run full verification and record residual risks

**Files:**
- Modify: plan checkboxes only if keeping execution notes in this file

- [ ] **Step 1: Run targeted unit tests for the new privacy domain package**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.domain.privacy.*"`

Expected: all privacy domain tests pass.

- [ ] **Step 2: Run targeted presentation tests for wallet, transaction, and UTXO detail surfaces**

Run: `bash ./gradlew :app:testDebugUnitTest --tests "com.strhodler.utxopocket.presentation.wallets.detail.*Privacy*"`

Expected: privacy-related ViewModel/reducer tests pass.

- [ ] **Step 3: Run required repository verification**

Run: `bash ./gradlew lintDebug`

Expected: `BUILD SUCCESSFUL` with no new lint regressions.

- [ ] **Step 4: Run required unit suite**

Run: `bash ./gradlew :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` with no failing unit tests.

- [ ] **Step 5: Review docs parity before calling the work complete**
  - user-visible copy updated
  - wiki/docs updated
  - skill routing updated
- [ ] **Step 6: Capture residual risks in the final report**
  - heuristic false positives/false negatives
  - local-only blind spots
  - deep-analysis still intentionally absent

---

## Deferred To Phase 2+

- Ancestry/descendancy tracing
- Entity detection and known-cluster lookups
- First-degree cluster walking
- Entropy/Boltzmann analysis
- Persisted findings and cache invalidation strategy
- Public score/grade model
- Optional deep-analysis mode using extra network context

## Deep-Analysis Design Seam

If a future phase needs heavier analysis, extend the domain layer through `PrivacyAugmentedContext` rather than rewriting the analyzers. That future work must be:

- explicit opt-in
- Tor-only and fail-closed
- documented as a privacy/trust trade-off
- separately tested for caching, rate limiting, and copy accuracy

## Suggested Execution Order

1. Chunk 1: domain foundation
2. Chunk 2: wallet analysis first
3. Chunk 3: transaction detail
4. Chunk 3: UTXO detail
5. Chunk 4: strings and docs
6. Chunk 4: specialist skill
7. Chunk 5: final verification
