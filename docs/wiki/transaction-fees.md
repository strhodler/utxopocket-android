---
id: transaction-fees
title: Transaction fees explained
summary: How transaction weight, feerate targets, and urgency interact when planning Bitcoin spends.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [mempool-fees, fee-selection-playbook, tx-anatomy, coin-selection-algos]
glossary_refs: [feerate, vbytes, mempool]
keywords: [fees, feerate, vbytes]
---

## Fee basics
Bitcoin fees are paid per transaction weight, not as a percentage of value. Input count, script type, and change outputs drive size.

## Choosing targets
Urgent payments require higher feerates during congestion. Flexible payments can wait for lower-fee windows, reducing long-term costs.

## Planning behavior
Good fee policy starts at draft time: select inputs carefully, avoid unnecessary change, and keep bump options available.

## Action checklist
- [ ] Estimate transaction weight before finalizing.
- [ ] Set feerate by urgency tier.
- [ ] Re-evaluate if confirmation lags expectations.
