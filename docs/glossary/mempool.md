---
id: mempool
title: Mempool
summary: Each node’s queue of unconfirmed transactions used to estimate feerates and relay policy.
related: [fee-selection-playbook, rbf-cpfp-strategies]
keywords: [fees, policy]
---

The mempool is a node’s local cache of unconfirmed transactions. Contents and policies vary between nodes, so fee estimation should rely on your own node (preferably via Tor). Deep mempools require higher feerates for timely confirmation; shallow mempools allow cheaper broadcasts.

