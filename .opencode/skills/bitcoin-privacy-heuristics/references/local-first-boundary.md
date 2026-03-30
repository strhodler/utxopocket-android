# Local-First Boundary (v1)

This boundary is mandatory for privacy heuristics in UtxoPocket v1.

## Allowed inputs

- Wallet analysis snapshots already present in memory (`WalletDetail`, UTXO lists, transaction summaries).
- Transaction detail state already loaded for the selected transaction.
- UTXO detail state and existing canvas/collection metadata already loaded in memory.
- Existing local labels and collection assignments.

## Forbidden inputs

- New network calls of any type (explorer APIs, mempool APIs, remote classifiers, entity lookups).
- Ancestry/descendancy tracing beyond already-synced local transaction context.
- New repositories, caches, background workers, or persistence just for findings.
- Automatic deep-analysis toggles that run without explicit user opt-in.

## Wording constraints

- Deterministic findings may state facts directly.
- Heuristic findings must use probabilistic wording and confidence labels.
- CoinJoin-like and similar pattern findings must remain informational and non-attributive.

## Future deep-analysis seam

- `PrivacyAugmentedContext` is the only intended extension seam for heavier analysis.
- Any future deep-analysis mode must be explicit opt-in and documented as a privacy/trust trade-off.
- Future work must preserve watch-only and Tor/fail-closed guarantees and include dedicated tests/docs.
