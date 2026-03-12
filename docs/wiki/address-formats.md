---
id: address-formats
title: Bitcoin address formats
summary: A practical map of legacy, SegWit, and Taproot address formats and how they affect fees, compatibility, and privacy.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [address-and-uri-standards, address-format-fingerprints, bech32-vs-bech32m, transaction-fees]
glossary_refs: [bech32, bech32m, script, vbytes]
keywords: [addresses, segwit, taproot]
---

## Main families
- Legacy (`1...`): broad compatibility, larger transactions.
- P2SH-wrapped SegWit (`3...`): transitional option for older senders.
- Native SegWit (`bc1q...`): lower weight and strong support.
- Taproot (`bc1p...`): modern script model and improved spend efficiency.

## Tradeoffs in practice
Format choice influences fee weight, software support, and fingerprinting. Mixing many script families in one spending pattern can make clustering easier.

## Watch-only guidance
Track which descriptor branch maps to each format and keep labels consistent. If you migrate formats, do it intentionally and document why.

## Action checklist
- [ ] Prefer native SegWit or Taproot for new receive flows.
- [ ] Confirm sender compatibility before issuing addresses.
- [ ] Keep script family labels in metadata.
