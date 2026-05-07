---
id: mempool-fees
title: Mempool behavior and fee pressure
summary: How mempool congestion changes confirmation odds and what fee tactics work under different conditions.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [fee-selection-playbook, transaction-fees, rbf-cpfp, mempool-variance-and-policy]
glossary_refs: [mempool, feerate, vbytes, rbf, cpfp]
keywords: [mempool, fees, confirmation]
---

## Dynamic market
The mempool is a live fee market. During congestion, low-fee transactions can stall while higher feerates clear first.

## Practical strategy
Set feerate by urgency, not habit. For non-urgent spends, wait for calmer windows. For urgent spends, plan replacement options up front.

## Policy caveat
Different nodes apply different relay policies, so propagation may vary before confirmation. Monitor state instead of relying on one broadcast view.

## Action checklist
- [ ] Use feerate targets tied to urgency tiers.
- [ ] Enable replacement path (RBF or CPFP planning).
- [ ] Reassess if unconfirmed beyond expected window.
