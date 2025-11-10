---
id: poisoned-utxos-quarantine
title: Quarantine for suspicious UTXOs
summary: Identify potentially poisoned coins and isolate them to avoid contaminating other compartments.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, address-reuse-casebook]
glossary_refs: [utxo, address-reuse, peel-chain]
keywords: [poisoned utxo, quarantine, segregation]
---

## Why it matters
Some coins arrive with tainted histories or suspicious patterns. Mixing them with clean compartments can leak identity or trigger unwanted scrutiny.

## Identification cues
- History tied to public incidents or known clusters.
- Outputs from mixers post‑processing that break typical spend patterns.
- Inbound change from merchants or services known to reuse addresses.

## Quarantine playbook
- Park the coin in a dedicated descriptor/compartment with explicit labels.
- Never co‑spend with other compartments.
- If spending is required, prefer exact spends that avoid change; if change occurs, keep it quarantined.

## Action checklist
- [ ] Label suspicious coins and move them to a quarantine bucket.
- [ ] Avoid merges; do not consolidate across buckets.
- [ ] Plan exits carefully (exact spend or separate disposal policy).

