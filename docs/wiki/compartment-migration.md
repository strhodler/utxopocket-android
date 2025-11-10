---
id: compartment-migration
title: Compartment migration
summary: Move coins between descriptors/buckets without linking identities; plan timing, fees, and change policy.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, consolidation-strategy, change-output-hygiene, provenance-labeling]
glossary_refs: [compartment, utxo, change-output, consolidation]
keywords: [migration, segregation, privacy]
---

## Why it matters
Sometimes you must move funds between compartments (e.g., retiring a descriptor, restructuring buckets). Poorly planned migrations can permanently link identities.

## Core guidance
- Define intent: why move and where; label both sides using BIP‑329.
- Prefer low‑activity windows and moderate fees to avoid standing out.
- Keep inputs from a single source compartment; avoid merges.
- Aim for exact spends to avoid creating change; if change is inevitable, return it to the same destination bucket and quarantine it.

## Practical steps
- Draft: select inputs from one compartment; simulate with realistic fees.
- Review: confirm destination descriptor, labels, and change policy.
- Execute: broadcast via your own backend; monitor confirmation.
- Post‑move: audit labels and set alerts to avoid co‑spends across old/new buckets.

## Action checklist
- [ ] One‑compartment inputs; labels updated.
- [ ] Fee and timing plan minimize attention.
- [ ] Change handled per destination policy.
- [ ] Post‑move audit completed.
