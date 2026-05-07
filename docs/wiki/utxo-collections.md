---
id: utxo-collections
title: UTXO collections
summary: Group UTXOs into named collections for clearer coin control, labeling, and spending intent.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, utxo-selection-heuristics, dust-policy-and-cleanup, provenance-labeling]
glossary_refs: [utxo, coin-control, compartment, dust]
keywords: [collections, grouping, coin control, dust]
---

## What collections are
Collections are named buckets of UTXOs. They give you an extra layer of organization on top of labels, so you can keep coins for the same purpose together and avoid accidental mixing.

Collections appear in the UTXO canvas, in the wallet collections list, and in the analysis views. Each collection shows how many UTXOs it contains and the total balance for that group.

## Create and assign collections
You can organize UTXOs in several ways:
- **UTXO canvas**: drag a UTXO onto another UTXO to create a new collection, or drag a UTXO onto an existing collection to add it.
- **Collection detail**: remove UTXOs to move them back into the unassigned pool.
- **UTXO detail**: assign a specific UTXO to a collection from its detail screen.

Collections that become empty are removed automatically to keep the canvas clean.

## The Dust collection
UtxoPocket maintains a built‑in **Dust** collection. It exists to keep tiny UTXOs grouped and easy to spot.

Key behaviors:
- The Dust collection appears automatically when the dust threshold is enabled and there are UTXOs below that threshold.
- It updates as the dust threshold or UTXO set changes.
- It is managed by the wallet and is not meant to be renamed.

Use the Dust collection as a quick way to review consolidation candidates without mixing them with higher‑value UTXOs.

## Practical tips
- Keep collections purpose‑based (income, savings, expenses, donations).
- Avoid dragging unrelated UTXOs into the same collection unless you accept the privacy link.
- Periodically review the Dust collection during low‑fee periods.

## Action checklist
- [ ] Create collections that mirror your spending intents.
- [ ] Assign new UTXOs as soon as they arrive.
- [ ] Review the Dust collection before consolidation or fee optimizations.
