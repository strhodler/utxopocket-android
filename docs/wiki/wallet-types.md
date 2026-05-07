---
id: wallet-types
title: Wallet types and trust boundaries
summary: Compare watch-only, hot, and hardware-assisted models with focus on security roles and operational fit.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [watch-only-threat-model, keys-and-seeds, transaction-signing, utxopocket-overview]
glossary_refs: [watch-only, seed-phrase, psbt]
keywords: [wallets, custody, security]
---

## Different roles, different risks
Wallet types vary by where keys live and who can authorize spending. Convenience and attack surface move together.

## Common categories
- Watch-only: monitoring and drafting, no signing keys.
- Hot wallet: signing keys on a networked device.
- Hardware-assisted flow: keys isolated in dedicated signer, often with PSBT transport.

## Recommended pattern
Use watch-only plus isolated signing for stronger safety and clearer audits.

## Action checklist
- [ ] Match wallet type to risk tolerance and use case.
- [ ] Keep signing privileges off daily-use phones.
- [ ] Document role boundaries for team setups.
