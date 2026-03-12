---
id: coin-control
title: Coin control for privacy and policy
summary: How deliberate input selection protects compartments, reduces surprises, and improves spend auditability.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [utxo-selection-heuristics, coin-selection-algos, change-output-hygiene, labeling-metadata]
glossary_refs: [coin-control, utxo, change-output, toxic-change]
keywords: [coin control, inputs, privacy]
---

## Why manual control exists
Automatic selection is convenient but can merge unrelated funds. Coin control lets you enforce purpose boundaries and avoid accidental clustering.

## Selection principles
- Spend within one compartment whenever possible.
- Prefer exact or near-exact matches to reduce change.
- Keep script and age patterns coherent for each policy bucket.

## Watch-only use
Use watch-only analysis to preview candidate inputs before exporting PSBTs. Label decisions so future audits explain why each input was chosen.

## Action checklist
- [ ] Pre-filter inputs by label and policy bucket.
- [ ] Review projected change before finalizing draft.
- [ ] Record rationale for unusual merges.
