---
id: tor-vs-vpn
title: Tor vs VPN for Bitcoin privacy
summary: Compare threat models, trust assumptions, and operational tradeoffs between Tor and VPN transport.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [why-tor, tor-integration, bitcoin-networking, operational-security]
glossary_refs: [tor, electrum-server]
keywords: [tor, vpn, privacy]
---

## Different trust shapes
A VPN hides traffic from local networks but centralizes trust in one provider. Tor distributes trust across relays and is purpose-built for anonymity routing.

## Bitcoin-specific perspective
For wallet query privacy, Tor usually offers stronger metadata protection than single-hop VPN routing, especially when paired with self-hosted endpoints.

## Practical nuance
VPNs can still be useful for connectivity or policy constraints, but they are not a substitute for Tor-level anonymity goals.

## Action checklist
- [ ] Choose transport based on threat model, not convenience alone.
- [ ] Prefer Tor for wallet metadata privacy.
- [ ] Avoid assuming VPN equals anonymity.
