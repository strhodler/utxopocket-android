---
id: hd-derivation
title: HD derivation paths explained
summary: How hierarchical derivation works, why path correctness matters, and how mistakes affect watch-only visibility.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [keys-and-seeds, descriptors-101, address-discovery-and-gap-limit, watch-only-restoration]
glossary_refs: [derivation-path, xpub, key-origin, gap-limit]
keywords: [bip32, derivation, xpub]
---

## Hierarchical model
HD wallets derive many keys from one seed tree. Paths encode account, branch, and index so wallets can recreate the same address set deterministically.

## Why path accuracy matters
A valid xpub with the wrong path can hide funds from watch-only scans or show unrelated history. Path and origin metadata must travel together.

## Operational guidance
Document path conventions per account type and test against known receive addresses after any migration.

## Action checklist
- [ ] Store key origin and derivation path together.
- [ ] Verify first receive addresses after import.
- [ ] Recheck gap-limit settings during restore.
