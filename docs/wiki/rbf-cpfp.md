---
id: rbf-cpfp
title: RBF and CPFP fundamentals
summary: When and how to use fee bumping mechanisms safely, with privacy-aware tradeoffs.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [rbf-cpfp-strategies, mempool-fees, transaction-fees, fee-selection-playbook]
glossary_refs: [rbf, cpfp, feerate, mempool]
keywords: [rbf, cpfp, fee bump]
---

## Two bumping paths
RBF replaces an unconfirmed transaction with a higher-fee version. CPFP spends a child output with high fee to pull a stuck parent into confirmation.

## Choosing between them
Use RBF when you control the original inputs and policy allows replacement. Use CPFP when replacement is unavailable but you can spend a descendant output.

## Privacy and policy notes
Fee bumping can reveal urgency and linking patterns. Apply the same compartment and labeling rules used for normal spends.

## Action checklist
- [ ] Plan bump strategy before first broadcast.
- [ ] Confirm replacement eligibility and constraints.
- [ ] Label bumped transactions for later analysis.
