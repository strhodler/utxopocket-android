---
id: wallet-analysis
title: Wallet Analysis overview
summary: Understand the Analysis section tabs (age, spendability, size bands, collections, treemap) and how to read them without leaking extra clues.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [analysis-age-distribution, utxo-collections, utxo-selection-heuristics, amount-analysis-heuristics, utxo-lifecycle-audit]
glossary_refs: [utxo, coin-control, descriptor]
keywords: [analysis, distributions, donut, treemap, collections]
---

## What the Analysis section shows
Each wallet has an **Analysis** area that visualizes the same UTXO set in several ways. Every tab supports both **Count** and **Value** modes so you can see whether volume or balance is concentrated in a bucket.

- **Age distribution** — Donut that groups UTXOs by confirmation age buckets (`<1 day` to `>2 years`).
- **Spendability split** — Donut separating spendable vs. non‑spendable (locked) UTXOs.
- **Size bands** — Donut that buckets UTXOs by value ranges (1k, 10k, 100k, 1M, 10M, 100M, 1B sats).
- **Collections** — Donut showing how many UTXOs (and how much value) sit in each collection, including the unassigned slice; colors match each collection’s color.
- **Treemap** — Tile view where each UTXO is sized by value and colored by age bucket; focus range controls keep all tiles visible but de‑emphasize those outside the selected band.

Empty states appear when there is no data (e.g., a new wallet still syncing).

## How to read and interact
- Tap a donut slice to highlight it; the legend scrolls to the matching card. Tap the card to reselect.
- Switch **Count/Value** to spot imbalances (e.g., few UTXOs holding most value).
- In Collections, use the color cue to match slices to the canvas cards; “Unassigned” uses the neutral surface variant.
- In Treemap, presets and the dual slider adjust the focus band; outside‑band tiles keep a faint outline so you retain full context.

## Privacy and safety notes
- All calculations are local to your device using the synced descriptors; nothing is sent out.
- Buckets are intentionally coarse to limit fingerprinting while still exposing consolidation or clustering risks.
- If a slice looks wrong, confirm the node is fully synced and labels/collections are current before taking action.
