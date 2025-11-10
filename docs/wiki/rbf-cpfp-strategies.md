---
id: rbf-cpfp-strategies
title: RBF and CPFP strategies
summary: When and how to use Replace‑by‑Fee or Child‑Pays‑for‑Parent to adjust confirmation time without breaking privacy.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, utxo-segregation-playbook, mempool-variance-and-policy]
glossary_refs: [rbf, cpfp, mempool]
keywords: [rbf, cpfp, fees, mempool]
---

## Concepts
- **RBF (Replace‑by‑Fee)**: broadcast a replacement transaction with higher fees that spends at least the same inputs and pays at least the same outputs (policy varies by node), signaling replaceability at creation.
- **CPFP (Child‑Pays‑for‑Parent)**: spend your own unconfirmed change with a high‑fee child to pull the parent across the line; miners evaluate package feerate.

## When to use which
- Choose RBF if the original spend is yours and marked replaceable—cleaner, fewer new outputs.
- Choose CPFP if RBF was not enabled or the wallet/signer cannot produce a replacement, but you control the change output.

## Privacy safeguards
- Keep replacements within the same compartment; do not introduce inputs from other buckets when crafting an RBF.
- For CPFP, ensure the child spends only the unconfirmed change that belongs to the same descriptor and returns change back to it.
- Maintain identical payment amounts and labels on RBF replacements; avoid altering destinations unless canceling safely is the intent.

## Practical steps
1. Inspect mempool drift and pick a new target feerate (see the fee selection playbook).
2. For RBF: rebuild the transaction with higher feerate and broadcast over Tor.
3. For CPFP: create a child that spends the unconfirmed change with a sufficiently high feerate so that the package (parent+child) meets the target.
4. Record labels: reference the parent txid, reason, and feerates for traceability.

## Risks and etiquette
- Frequent small bumps create noise; estimate once, then bump decisively.
- Avoid CPFP chains unless necessary; each child increases complexity and fees.
- Respect recipient expectations; unexpected replacements can confuse automated monitors.

## Action checklist
- [ ] Prefer RBF when available; fall back to CPFP using your own change.
- [ ] Keep all inputs/outputs within the same descriptor bucket.
- [ ] Update labels on replacements with txids and new feerates.
- [ ] Recheck mempool after broadcasting to confirm propagation.
