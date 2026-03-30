---
name: bitcoin-privacy-heuristics
description: Use when implementing, reviewing, or documenting wallet privacy heuristics so deterministic facts, probabilistic inferences, and local-first boundaries remain explicit.
---

# Bitcoin Privacy Heuristics Specialist

Use this skill for changes to privacy finding logic, wording, tests, or docs.

## Baseline failure modes (RED)

Before this skill, agents repeatedly failed in these scenarios:
- **Overstated certainty:** described probable change and multi-input ownership as confirmed ownership facts.
- **Pattern overclaiming:** treated CoinJoin-like equal outputs as definitive attribution instead of informational context.
- **Boundary drift:** proposed explorer/entity lookups or ancestry tracing in v1 despite local-first scope.

These failures are the reason this skill exists.

## Core rules

1. Deterministic vs heuristic language is mandatory
- Deterministic findings may use direct wording (`detected`, `is`, `contains`) only for observable local facts.
- Heuristic findings must use cautious wording (`probable`, `suggests`, `may`, `can`).
- Never collapse confidence labels into certainty claims.

2. Exception-aware interpretation
- CoinJoin-like, PayJoin-like, collaborative spends, and multisig policies can invalidate naive ownership/change assumptions.
- Any inference that could be invalidated by these patterns must stay explicitly probabilistic.

3. Local-first boundary is hard
- Use only already-loaded app data available in current domain/UI snapshots.
- Do not add explorer calls, mempool lookups, entity databases, ancestry tracing, or external classifiers in v1.
- Do not persist findings in Room/DataStore for v1.

4. No hidden network assumptions
- Do not imply background enrichment, remote scoring, or remote attribution.
- If a future deep-analysis mode is discussed, mark it as inactive and explicit opt-in only.

## Heuristic change checklist

When adding or modifying any finding id:
- Update analyzer logic and `CrossHeuristicRules` as needed.
- Add/adjust unit tests for positive, negative, and suppression/escalation paths.
- Update string mappings in English first, then mirrored localized entries with placeholder parity.
- Update docs that expose user-facing behavior (`README.md`, wiki pages, `knowledge/features/privacy-heuristics.md`).
- Update this skill references if the catalog or boundaries changed.

## Review prompts

Use these checks during PR review:
- "Does any heuristic statement read like certainty?"
- "Could CoinJoin/PayJoin/multisig make this statement wrong?"
- "Did we keep analysis local-first with no new network assumptions?"
- "Are docs and localized strings synchronized with implementation?"

## Retest outcomes (GREEN)

After applying these rules, the same pressure scenarios should pass:
- Heuristic claims keep probabilistic wording.
- CoinJoin-like findings remain informational with suppressions where applicable.
- v1 proposals stay within local-first inputs and do not add network enrichment.

## References

- `references/heuristic-catalog.md`
- `references/local-first-boundary.md`
