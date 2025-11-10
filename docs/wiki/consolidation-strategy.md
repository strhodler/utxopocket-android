---
id: consolidation-strategy
title: Consolidation strategy
summary: Plan low‑fee windows to consolidate safely, minimize fingerprints, and prepare for future spends.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, utxo-segregation-playbook, change-output-hygiene]
glossary_refs: [consolidation]
keywords: [consolidation, fees, privacy]
---

## Why it matters
Consolidation reduces future fees by merging small UTXOs into larger ones. Done carelessly, it links compartments and creates long‑lived fingerprints. Done well, it lowers costs without sacrificing privacy.

## When to consolidate
- During mempool lulls: Feerates are low; schedule batches then, not during peaks.
- Per bucket: Consolidate inside a single descriptor/purpose to avoid cross‑links.
- After labeling: Ensure provenance labels exist so merged history remains auditable.

## How to consolidate privately
- Keep inputs from one compartment and one script family.
- Target outputs that avoid creating dust; one well‑sized output is often best.
- If future spends will require exact amounts, consider multiple outputs sized to likely payments within the same bucket.

## Operational safeguards
- Never mix KYC/non‑KYC stacks or different jurisdictions in one consolidation.
- Run through policy checks (standardness, feerate) to avoid stuck transactions.
- Label the intent (BIP‑329) and note the fee window used for later audits.

## Action checklist
- [ ] Choose a low‑fee window based on your node’s mempool view.
- [ ] Consolidate per bucket and script type only.
- [ ] Avoid dust, and size outputs for future needs.
- [ ] Record labels and confirm post‑consolidation balances.

