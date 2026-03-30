# Privacy Heuristics

## Goal
Document the current local-first privacy heuristics feature exposed in wallet analysis, transaction detail, and UTXO detail.

## Current architecture
- Domain layer analyzers:
  - `WalletPrivacyAnalyzer`
  - `TransactionPrivacyAnalyzer`
  - `UtxoPrivacyAnalyzer`
- Shared contracts and orchestration:
  - `PrivacyFinding`, `PrivacySummary`, and `PrivacyAugmentedContext`
  - `CrossHeuristicRules` for suppression, deduplication, and escalation
- Presentation wiring:
  - ViewModels compute findings from already-available screen snapshots
  - Reducers and composables render summaries/findings only
  - Copy resolves from finding ids into Android string resources

## Supported heuristics (v1)

### Wallet scope
- Address reuse exposure
- Dust pressure and toxic-change risk
- Fragmentation pressure
- Mixed script/address family fingerprints
- Label and collection hygiene gaps
- Positive signals (low reuse, low dust, organized labels)

### Transaction scope
- Multi-input ownership exposure (heuristic)
- Consolidation and self-transfer patterns
- Probable change exposure (heuristic)
- Changeless spend positive signal
- Mixed address family fingerprint
- CoinJoin-like equal-output informational pattern

### UTXO scope
- Reused receive address exposure
- Change-origin context
- Dust or near-dust warning
- Missing label/collection organizational risk
- Spendability or maturity informational context

## Local-first boundary (v1)
- Findings are computed on-device from already-synced wallet data only.
- No new repositories, persistence, caches, background jobs, or network lookups are added for this feature.
- Findings are ephemeral UI/domain output and are not persisted in Room/DataStore.

## Out of scope in v1
- Ancestry or descendancy tracing
- Entity or cluster lookups
- Entropy/Boltzmann scoring
- Public numeric privacy score/grade
- Any automatic deep-analysis mode

## Future deep-analysis seam (inactive)
- `PrivacyAugmentedContext` is reserved as the extension seam for future deep analysis.
- Any deep-analysis mode must be explicit opt-in, separately documented, and reviewed as a privacy/trust trade-off before release.
- If implemented later, it must preserve watch-only guarantees, Tor/fail-closed networking policy, and include dedicated tests/docs for new heuristics.

## Related public docs
- `README.md`
- `docs/wiki/wallet-analysis.md`
- `docs/wiki/bitcoin-privacy.md`
- `SECURITY.md`
