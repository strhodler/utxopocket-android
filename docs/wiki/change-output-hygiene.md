---
id: change-output-hygiene
title: Change output hygiene
summary: Recognize and contain toxic change so compartments stay isolated and future spends remain private.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, fee-selection-playbook, address-reuse-casebook]
glossary_refs: [change-output, toxic-change, coin-control]
keywords: [change, toxic change, coin control]
---

## What is “toxic change”?
Any change output that links unrelated histories or wallet policies (e.g., sending from a ‘donations’ bucket and returning change to ‘savings’) becomes toxic. Once mixed, later co‑spends leak connections across compartments.

## How to spot it
- Script and path: ensure the change script/policy matches the source descriptor.
- Amount patterns: odd leftovers that force future merges can create peel chains.
- Label gaps: missing provenance on change makes later selection error‑prone.

## Containment strategies
1. Always return change to the same descriptor bucket that funded the spend.
2. Use coin control to avoid inputs that would force cross‑bucket merges.
3. If change becomes toxic, quarantine it in a dedicated descriptor and avoid co‑spends with clean buckets.
4. Schedule consolidation windows (low fees) to tidy small change before it turns into dust.

## Drafting spends safely
- Preview the candidate change: policy, path, and label before signing/sending.
- Prefer exact‑match inputs when possible to reduce or eliminate change.
- If the wallet supports it, set rules that block change from leaving the bucket.

## Action checklist
- [ ] Verify change script/policy and destination descriptor before broadcasting.
- [ ] Label change with provenance and intent at creation time.
- [ ] Quarantine suspicious change and avoid co‑spends across buckets.
- [ ] Consolidate during mempool lulls; keep change manageable.
