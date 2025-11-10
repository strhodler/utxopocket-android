---
id: peel-chain-patterns
title: Peel‑chain patterns
summary: Recognize and avoid peel chains that leak long‑term spending behavior and link clusters.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [change-output-hygiene, consolidation-strategy, address-reuse-casebook]
glossary_refs: [peel-chain, change-output]
keywords: [peel chain, change, privacy]
---

## Why it matters
Peel chains arise when you repeatedly spend from a large UTXO and take change back, creating a linkable trail across many payments.

## How they form
- Reusing the same source UTXO across multiple spends.
- Always returning change to the same descriptor without quarantine.
- Address reuse or narrow script families that make clustering easy.

## Mitigations
- Favor exact spends from appropriately sized UTXOs.
- Quarantine change and plan consolidations separately during low‑fee windows.
- Rotate funding sources across compartments to avoid long chains.

## Action checklist
- [ ] Avoid repeated change reuse.
- [ ] Prefer exact spends when safe.
- [ ] Consolidate thoughtfully and label outcomes.
