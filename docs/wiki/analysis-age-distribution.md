---
id: analysis-age-distribution
title: Analysis: age distribution
summary: How each wallet’s Analysis tab visualizes UTXO ages, what the buckets mean, and how to read the donut and legend safely.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-selection-heuristics, utxo-lifecycle-audit, amount-analysis-heuristics]
glossary_refs: [utxo, descriptor, derivation-path]
keywords: [analysis, utxo age, histogram, donut, privacy]
---

## What the screen shows
The Analysis tab inside each wallet detail now offers multiple breakdowns over the same wallet snapshot:
- **Age distribution (donut)** — groups every UTXO into age buckets (from `<1 day` up to `>2 years`) and shows their relative share. You can toggle between **Count** and **Value** to see how many UTXOs or how much balance sits in each bucket.
- **Spendability split** — a donut that separates spendable vs. locked/unspendable outputs so you know what is actionable.
- **Value bands** — a donut that buckets UTXOs by rounded value bands (1k, 10k, 100k, 1M, 10M, 100M, 1B sats) matching the treemap shortcuts.
- **Hold waves** — a time series of the same buckets to spot aging trends. Empty wallets show a placeholder; data appears once the node finishes syncing descriptors.

## How to read the donut
- Tap a slice to highlight it; the matching legend card scrolls into view and shows the same color badge. Tap the card to reselect a slice.
- Bucket colors are stable per snapshot so you can cross‑reference the donut with the legend at a glance.
- Switch between **Count** and **Value** to catch imbalances (e.g., few old UTXOs holding most of the value).

### Treemap (UTXO-map)
- Shows every UTXO as a tile sized by value; color is fixed to age buckets for clarity. No text labels on the canvas—tap a tile to open details.
- Use quick presets (aligned to the value bands above) or the dual-handle slider to focus on a value range; the layout reflows instantly and tiny UTXOs are aggregated into a single tappable bin per color.
- The treemap lives alongside the other distribution tabs inside Analysis so you can move between views without leaving the wallet.

## Privacy and security notes
- Everything is computed locally from the synced watch‑only data; no metrics are sent out of the app.
- The buckets are coarse on purpose to avoid revealing exact times while still surfacing consolidation risks.
- Age is derived from confirmation height; unconfirmed UTXOs are excluded until mined.

## When to act
- **Too many fresh UTXOs**: consider pausing spends to avoid creating obvious change trails.
- **Value concentrated in old buckets**: plan gradual spends or pre‑build PSBTs so you don’t leak long‑dormant coins in one transaction.
- **Empty buckets where you expect flow**: confirm the node is synced and labels are correct before assuming coins are missing.

## Operator checklist
- [ ] Pick the right mode (`Count` vs `Value`) before sharing screenshots internally.
- [ ] Verify the descriptor set is fully synced; partial sync skews bucket sizes.
- [ ] Note any sharp shifts in hold waves and link them to specific drafts or consolidations.
