---
id: xpub
title: Extended public key (xpub)
summary: A BIP32 extended public key that derives addresses for a wallet branch without revealing private keys.
related: [watch-only-restoration, descriptors-advanced, descriptor-maps-and-recovery]
keywords: [xpub, bip32, derivation]
---

An xpub contains the public key, chain code, and depth/child data for hierarchical derivation. In watch‑only setups, descriptors reference xpubs with origin data to derive external and change addresses deterministically across apps.

