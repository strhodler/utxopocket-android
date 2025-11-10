---
id: utxo-selection-heuristics
title: UTXO selection heuristics
summary: Input selection strategies (knapsack, branch‑and‑bound) and privacy‑aware constraints for real spends.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, change-output-hygiene, rbf-cpfp-strategies]
glossary_refs: [knapsack, branch-and-bound]
keywords: [coin selection, inputs, privacy]
---

## Why it matters
Which UTXOs you spend controls fees, fingerprints, and privacy. Good selection hits the target amount with minimal inputs while respecting compartment boundaries and change policy.

## Common strategies
- Knapsack: Greedy or heuristic packing of inputs to reach a target; simple but can over‑select and create toxic change.
- Branch‑and‑bound: Systematically searches combinations to hit an exact target or near‑optimal set; fewer inputs, but may be slower.

## Privacy‑aware constraints
- Do not merge compartments just to reduce inputs; privacy costs more than a few sats.
- Prefer exact matches that avoid change, but only within the same bucket and without revealing old, dormant UTXOs unnecessarily.
- Avoid pulling tiny UTXOs at peak fees; plan separate consolidation windows instead.

## Practical guidance
- Pre‑filter candidate inputs by bucket, script type, and recent activity to reduce leaks.
- Favor smaller input counts for lower fees and simpler fingerprints, but avoid introducing small toxic change.
- If change is inevitable, return it to the same descriptor/bucket and label intent per BIP‑329.

## Action checklist
- [ ] Filter candidates to one compartment and script family.
- [ ] Attempt exact/near‑exact selection to avoid change when safe.
- [ ] Minimize input count without merging buckets.
- [ ] Verify change policy and labels if produced.
