---
id: why-tor
title: Why Tor matters for Bitcoin wallets
summary: The privacy rationale for Tor routing in wallet operations and what protections it does and does not provide.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [tor-integration, tor-vs-vpn, electrum-servers, bitcoin-networking]
glossary_refs: [tor, electrum-server, anonymity-set]
keywords: [tor, privacy, metadata]
---

## Metadata is part of security
Even if funds are safe cryptographically, network metadata can reveal patterns about your holdings and behavior. Tor reduces direct source attribution.

## What Tor helps with
- Hides client network origin from wallet endpoints.
- Makes routine query profiling harder for observers.
- Supports safer use of remote infrastructure.

## What Tor does not fix
Tor cannot undo on-chain mistakes like address reuse or poor UTXO hygiene. Transport privacy and transaction privacy must be managed together.

## Action checklist
- [ ] Keep Tor enabled for wallet networking by default.
- [ ] Combine Tor with disciplined labeling and coin control.
- [ ] Treat Tor outages as privacy events and stop sync until transport is restored.
