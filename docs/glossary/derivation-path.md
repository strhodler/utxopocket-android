---
id: derivation-path
title: Derivation path (BIP32)
summary: Notation that specifies how keys and addresses are derived in a hierarchical wallet (e.g., m/84'/0'/0'/0/0).
related: [watch-only-restoration, descriptors-advanced]
keywords: [bip32, path, account]
---

Correct derivation paths ensure watch‑only scanners look in the right place for funds. Descriptors encode both the account path and branch (`/0/*` external, `/1/*` change) so different tools can reproduce the same address set.

