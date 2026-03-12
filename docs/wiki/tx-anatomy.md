---
id: tx-anatomy
title: Bitcoin transaction anatomy
summary: Inputs, outputs, change, and witness data explained from a wallet operator perspective.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [utxo-basics, transaction-fees, change-output-hygiene, psbt-explained]
glossary_refs: [utxo, outpoint, change-output, vbytes]
keywords: [transaction, inputs, outputs]
---

## Core structure
A transaction spends previous outputs as inputs and creates new outputs. Any unspent value becomes a new UTXO, often including change back to the sender.

## Why structure matters
Input and output choices affect fee cost, privacy leakage, and future spend flexibility. Transaction design is not only a technical detail.

## Watch-only review value
Before signing, inspect input origin, destination outputs, and change placement. Most preventable mistakes are visible at this stage.

## Action checklist
- [ ] Verify each output purpose before approval.
- [ ] Confirm change returns to intended descriptor branch.
- [ ] Avoid unnecessary input merges.
