---
id: rbf
title: Replace-by-Fee (RBF)
summary: A policy allowing a higher-fee replacement of an unconfirmed transaction to speed up confirmation.
related: [rbf-cpfp-strategies]
keywords: [fees, replace]
---

RBF lets a sender rebroadcast a new version of an unconfirmed transaction with a higher feerate. Wallets typically must mark the original as replaceable. Use RBF to adjust timing without introducing new inputs that would leak compartments.

