---
id: wallet-analysis
title: Wallet Analysis overview
summary: Understand Analysis tabs and wallet privacy findings, and interpret local-only heuristics without treating them as ground truth.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [bitcoin-privacy, analysis-age-distribution, utxo-collections, utxo-selection-heuristics, amount-analysis-heuristics]
glossary_refs: [utxo, coin-control, descriptor]
keywords: [analysis, privacy findings, distributions, donut, treemap, collections]
---

## What the Analysis section shows
Each wallet has an **Analysis** area that combines local charts with privacy review hints from the same wallet snapshot. Every tab supports both **Count** and **Value** modes so you can see whether volume or balance is concentrated in a bucket.

- **Privacy snapshot** — Summary counts by severity plus wallet-level privacy findings. These findings are review aids, not proof of ownership intent.
- **Transaction and UTXO privacy sections** — Matching finding cards also appear in transaction detail and UTXO detail for context-specific review.

- **Age distribution** — Donut that groups UTXOs by confirmation age buckets (`<1 day` to `>2 years`).
- **Spendability split** — Donut separating spendable vs. non‑spendable (locked) UTXOs.
- **Size bands** — Donut that buckets UTXOs by value ranges (1k, 10k, 100k, 1M, 10M, 100M, 1B sats).
- **Collections** — Donut showing how many UTXOs (and how much value) sit in each collection, including the unassigned slice; colors match each collection’s color.
- **Treemap** — Tile view where each UTXO is sized by value and colored by age bucket; focus range controls keep all tiles visible but de‑emphasize those outside the selected band.

Empty states appear when there is no data (e.g., a new wallet still syncing).

## Local-only boundary for privacy findings
- Findings are computed on-device from already-synced wallet data and current labels/collections.
- No new network calls, explorer lookups, entity databases, or ancestry tracing are used in this v1 surface.
- Severity and confidence indicate review priority; low/medium confidence findings remain heuristics.

## How to read and interact
- Start with severity and confidence tags, then inspect the card evidence before changing spend plans.
- Use wallet findings to spot broad patterns, then confirm details in transaction/UTXO screens.
- Treat "probable change" and similar wording as inference, not certainty.
- Prefer label/collection cleanup first when organization findings appear.
- Tap a donut slice to highlight it; the legend scrolls to the matching card. Tap the card to reselect.
- Switch **Count/Value** to spot imbalances (e.g., few UTXOs holding most value).
- In Collections, use the color cue to match slices to the canvas cards; “Unassigned” uses the neutral surface variant.
- In Treemap, presets and the dual slider adjust the focus band; outside‑band tiles keep a faint outline so you retain full context.

## Privacy and safety notes
- All analysis and findings are local to your device using synced descriptors; nothing is sent out.
- Buckets are intentionally coarse to limit fingerprinting while still exposing consolidation or clustering risks.
- If a finding looks wrong, confirm node sync status plus labels/collections before taking action.
