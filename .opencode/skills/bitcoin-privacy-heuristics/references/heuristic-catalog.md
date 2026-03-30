# Privacy Heuristic Catalog (v1)

Use this catalog when reviewing wording, confidence, and test expectations.

## Wallet scope

| Finding id | Class | Typical confidence | Notes |
| --- | --- | --- | --- |
| `wallet-address-reuse` | Deterministic | Deterministic | Directly observed address reuse count. |
| `wallet-dust-pressure` | Deterministic fact + risk framing | High | Dust counts are factual; future pressure is interpretive. |
| `wallet-fragmentation-pressure` | Heuristic | Medium | Threshold-based consolidation pressure hint. |
| `wallet-toxic-change-risk` | Heuristic | Medium | Uses probable-change style assumptions over local outputs. |
| `wallet-label-hygiene-gap` | Deterministic | High | Label/collection coverage from local metadata. |
| `wallet-mixed-script-families` | Deterministic observation | High | Mixed script families observed in local snapshot. |
| `wallet-mixed-address-families` | Deterministic observation | High | Mixed address families observed in local snapshot. |
| `wallet-low-reuse` | Deterministic positive | High | No reuse detected in analyzed set. |
| `wallet-organized-labels` | Deterministic positive | High | Label coverage passes positive threshold. |
| `wallet-low-dust` | Deterministic positive | High | Dust threshold not triggered in analyzed set. |

## Transaction scope

| Finding id | Class | Typical confidence | Notes |
| --- | --- | --- | --- |
| `transaction-multi-input-ownership` | Heuristic | Low to Medium | Ownership clustering inference only. |
| `transaction-consolidation-fan-in` | Heuristic | Medium | Pattern-based consolidation interpretation. |
| `transaction-probable-change` | Heuristic | Medium | Probable change output inference. |
| `transaction-changeless-spend` | Heuristic positive | Medium | Pattern suggests lower immediate change exposure. |
| `transaction-self-transfer` | Deterministic observation | High | All outputs observed as wallet-owned in snapshot. |
| `transaction-change-detected` | Reserved/inactive | N/A | Keep mapping/docs ready; currently not emitted by analyzers. |
| `transaction-address-linkability` | Deterministic observation + risk framing | High | Mixed family detection is factual; risk remains contextual. |
| `transaction-coinjoin-pattern` | Heuristic informational | Low to Medium | Equal-output pattern only; never attribution proof. |

## UTXO scope

| Finding id | Class | Typical confidence | Notes |
| --- | --- | --- | --- |
| `utxo-dust-warning` | Deterministic | High | Value compared to dust/near-dust thresholds. |
| `utxo-address-reuse` | Deterministic | High | Local reuse count for the UTXO address. |
| `utxo-change-origin` | Deterministic observation | High | Descriptor/change-branch context. |
| `utxo-organization-gap` | Deterministic | High | Missing local label and collection assignment. |
| `utxo-spendability-context` | Deterministic informational | High | Current spendability/maturity context. |

## Catalog maintenance rules

- Add new finding ids here in the same change set as analyzer and string updates.
- If class changes (deterministic vs heuristic), update wording and tests together.
- Do not introduce public score semantics in this catalog for v1.
