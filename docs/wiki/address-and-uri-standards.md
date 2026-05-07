---
id: address-and-uri-standards
title: Address and URI standards
summary: How Bitcoin addresses and bitcoin: URIs are encoded, validated, and shared safely in watch-only workflows.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [address-formats, bech32-vs-bech32m, tx-anatomy, utxopocket-overview]
glossary_refs: [bech32, bech32m, script]
keywords: [address, uri, bip21]
---

## Why standards matter
Address and URI standards reduce copy errors and wallet mismatches. A valid format does not guarantee it matches your intent, so verify network, amount, and label before sharing or paying.

## Practical rules
- Prefer native SegWit and Taproot formats when counterparties support them.
- Treat `bitcoin:` URIs as helpers, not authority. Review every field before approving.
- Reject malformed or mixed-network addresses immediately.

## UtxoPocket perspective
UtxoPocket is watch-only, so it verifies and displays receive details but never signs. Use it to check destination hygiene, then sign in a separate signer workflow.

## Action checklist
- [ ] Confirm address network matches your wallet network.
- [ ] Validate URI amount and label/message fields before sharing or paying.
- [ ] Avoid reusing old receive addresses.
