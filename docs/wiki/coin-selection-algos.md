---
id: coin-selection-algos
title: Coin selection algorithms
summary: How common input selection algorithms behave and where human policy overrides are essential.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [coin-control, utxo-selection-heuristics, transaction-fees, mempool-fees]
glossary_refs: [branch-and-bound, knapsack, feerate, vbytes]
keywords: [coin selection, algorithms, fees]
---

## Algorithms are policy tools
Coin selection methods optimize different goals: fewer inputs, less change, or lower immediate fees. None can infer your privacy intent automatically.

## Typical behaviors
- Knapsack-like methods are fast but may over-aggregate inputs.
- Branch-and-bound seeks tighter matches and less change at higher compute cost.
- Fallback logic may create small change when exact sets fail.

## Human override points
Apply policy filters before algorithmic selection: compartment, script family, and spending urgency. This keeps optimization from violating privacy rules.

## Action checklist
- [ ] Define pre-filters before running selection.
- [ ] Compare two candidate drafts, not just one.
- [ ] Reject drafts that cross risk boundaries for minor fee savings.
