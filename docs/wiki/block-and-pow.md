---
id: block-and-pow
title: Blocks and proof of work
summary: Why proof of work secures ordering and scarcity, and how confirmations relate to finality risk.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [confirmation-policy, transaction-standardness-vs-consensus, tx-anatomy, mempool-fees]
glossary_refs: [confirmation, mempool]
keywords: [blocks, mining, pow]
---

## What proof of work does
Proof of work makes block history expensive to rewrite. The deeper a transaction is buried under new blocks, the harder it is to reverse.

## Confirmations and risk
Confirmations are a risk metric, not an absolute guarantee. Required depth depends on value, counterparty trust, and your reorg tolerance.

## Operational implication
Treat zero-confirmation events as provisional. Build policy thresholds that match your threat model instead of using one universal rule.

## Action checklist
- [ ] Define confirmation targets by payment size.
- [ ] Re-check status during high volatility.
- [ ] Avoid irreversible actions on 0-conf signals alone.
