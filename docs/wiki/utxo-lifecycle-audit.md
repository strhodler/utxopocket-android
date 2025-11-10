---
id: utxo-lifecycle-audit
title: UTXO lifecycle audit
summary: Track a coin from acquisition to disposal with labels, compartment rules, and end‑of‑life policies.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [provenance-labeling, utxo-segregation-playbook, consolidation-strategy]
glossary_refs: [utxo, bip-329, consolidation]
keywords: [lifecycle, labels, audit]
---

## Why it matters
An auditable lifecycle helps you avoid accidental merges and retain provenance for compliance or personal tracking without leaking unnecessary metadata.

## Stages and checks
- Acquisition: record source and intent with BIP‑329 labels; place in the correct compartment.
- Holding: monitor activity, avoid address reuse, and plan consolidations.
- Spending: select inputs within one bucket; prefer exact spends; handle change per policy.
- Disposal: archive labels and outcomes; review quarantined coins separately.

## Action checklist
- [ ] Every UTXO has a source and intent label.
- [ ] Compartment rules enforced at selection time.
- [ ] Consolidations planned and labeled.
- [ ] Final outcomes recorded for future reference.

