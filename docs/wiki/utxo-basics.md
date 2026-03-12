---
id: utxo-basics
title: UTXO basics
summary: Understand unspent outputs as spendable units and why UTXO management drives fee, privacy, and policy outcomes.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [tx-anatomy, coin-control, utxo-lifecycle-audit, transaction-fees]
glossary_refs: [utxo, outpoint, coin-control, change-output]
keywords: [utxo, wallet model, spending]
---

## What a UTXO is
Bitcoin wallets do not hold one mutable balance record. They track a set of unspent outputs that can be selected and spent later.

## Operational implications
How you accumulate, label, and combine UTXOs determines future fees and privacy quality. UTXO management is a first-class wallet skill.

## Watch-only advantage
Watch-only views make it easier to inspect UTXO history and intent before drafting spends.

## Action checklist
- [ ] Label UTXOs when received.
- [ ] Keep compartments separated by purpose.
- [ ] Review stale small outputs for planned consolidation.
