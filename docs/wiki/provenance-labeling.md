---
id: provenance-labeling
title: Provenance labeling (BIP‑329)
summary: A practical schema to capture where coins came from and how they should be used, preserved across apps.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, change-output-hygiene, label-export-bip329-workflows]
glossary_refs: [bip-329, coin-control, address-reuse]
keywords: [labels, provenance, bip-329]
---

## Why provenance matters
Future spending decisions depend on origin and intent. Without consistent labels, you risk merging unrelated histories or leaking sensitive associations.

## Minimal schema
- Source: `cash-p2p`, `exchange`, `salary`, `donation`.
- Intent: `savings`, `expenses`, `gift`, `donation`.
- Context: date, counterparty alias, jurisdiction hints if relevant.
- Technical: descriptor/account, change path, txid.

Use BIP‑329 JSON so labels can travel between wallets without manual rewrite.

## Capture at the right moment
- Label on receipt and when drafting a spend; retrofitting later is error‑prone.
- Pair labels with the correct descriptor bucket to avoid cross‑contamination.
- Keep an encrypted backup of exported labels together with descriptor maps.

## Migration tips
- Export BIP‑329 from the source app; import into the destination before rescanning.
- Verify a small subset: addresses match, labels attach to the expected UTXOs.
- If an app lacks BIP‑329, keep a portable CSV and convert later.

## Action checklist
- [ ] Define a simple schema (source, intent, date) and use it consistently.
- [ ] Export/import BIP‑329 during wallet migrations.
- [ ] Review labels quarterly and fix gaps before they cause bad coin selection.
