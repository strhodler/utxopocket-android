---
id: keys-and-seeds
title: Keys and seeds in a watch-only model
summary: Distinguish seeds, private keys, and public derivation data, and keep signing secrets off monitoring devices.
category_id: safety-operations
category_title: Safety and operations
category_description: Operational practices for resilient, private wallet use.
related: [watch-only-threat-model, hd-derivation, wallet-types, transaction-signing]
glossary_refs: [seed-phrase, passphrase, xpub, watch-only]
keywords: [seed, private key, watch-only]
---

## Separation of roles
Seeds and private keys authorize spending. Watch-only descriptors and xpubs provide visibility without spending power. Mixing these roles weakens security boundaries.

## Risk management
Treat seed material as offline secrets with minimal exposure windows. Monitoring devices should never become temporary signing endpoints.

## UtxoPocket posture
UtxoPocket accepts public-only wallet data and does not implement key import or signing. If the phone is compromised, an attacker can read wallet metadata but cannot sign with keys that were never on the device.

## Action checklist
- [ ] Keep seed backups offline and physically protected.
- [ ] Import only public descriptors into watch-only apps.
- [ ] Validate signer outputs using independent watch-only checks.
