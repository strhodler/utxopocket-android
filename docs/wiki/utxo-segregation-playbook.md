---
id: utxo-segregation-playbook
title: UTXO segregation playbook
summary: Keep coins in clear compartments, avoid toxic change, and spend with intent using labels and coin control.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-basics, labeling-metadata, bitcoin-privacy, change-output-hygiene]
glossary_refs: [utxo, change-output, address-reuse, coin-control]
keywords: [segregation, coin control, change, labels]
---

## The goal
Segregation means each UTXO belongs to a purpose bucket (income, savings, expenses, donations). You avoid co‑spending unrelated histories and reduce the blast radius of leaks.

## Practical rules
1. **One descriptor per bucket** — Watch‑only descriptors for each purpose. Do not merge buckets in the same spend unless you accept the link.
2. **Label the source** — Use BIP‑329 labels like `source: cash‑p2p, date: 2025‑11‑07`. Provenance informs future spending policy.
3. **Guard your change** — When spending, ensure change returns to the same bucket. Toxic change appears when you mix buckets or return change to a different policy.
4. **Avoid address reuse** — Your wallet should serve fresh receive addresses automatically; never share the same address with multiple parties.
5. **Consolidate on quiet mempools** — If you must merge small UTXOs, do it during lulls, with fresh change paths and clear labels.

## Spending with coin control
- Start from the “why”: pick inputs that match the payment’s purpose; do not let the wallet auto‑merge unrelated histories.
- Prefer fewer inputs (lower fees) but never at the expense of leaking compartments.
- Inspect the candidate change: if it links compartments, adjust inputs or split the payment.

## Action checklist
- [ ] Create a descriptor for each bucket (income, savings, expenses, donations).
- [ ] Add labels at receive time and when broadcasting.
- [ ] Practice a small coin‑control spend and verify that change returns to the right bucket.
- [ ] Schedule periodic consolidation windows for dust and tiny UTXOs.
