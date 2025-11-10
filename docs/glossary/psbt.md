---
id: psbt
title: PSBT (Partially Signed Bitcoin Transaction)
summary: Standard container for assembling, exchanging, and signing transactions across devices and apps without exposing private keys.
related: [rbf-cpfp-strategies, utxo-selection-heuristics]
keywords: [psbt, signing, hardware wallet]
---

A PSBT packages inputs, outputs, and metadata (key origins, scripts, signatures) so multiple tools can collaborate on a spend safely. Watch‑only wallets draft PSBTs using descriptors and labels, then export them to signers. See the fee and input selection guides for preparing clean, intent‑labeled PSBTs.

