---
id: privacy-threat-models
title: Threat Models
summary: Match your Bitcoin privacy controls to the adversary you actually face.
category_id: privacy-networking
category_title: Privacy & Networking
category_description: How to protect your privacy and understand how Bitcoin nodes communicate.
related: [watch-only-threat-model, block-explorer-privacy, node-connectivity]
glossary_refs: [tor, coin-control, watch-only]
keywords: [threat model, privacy, adversary, metadata, compartmentalization]
---

## Start with concrete adversaries
Privacy planning only works when you define who you are defending against. Typical profiles include casual observers, service providers with logs, chain-surveillance companies, and state-level actors with legal compulsion powers.

## Controls by adversary level
- **Casual observers**: strong device lock, no address reuse, clear wallet compartment labels.
- **Service-provider logs**: Tor-first connectivity, self-hosted nodes when possible, minimized KYC linkage.
- **Professional surveillance**: strict UTXO compartmentalization, deliberate coin control, avoid explorer leakage, use collaborative spending workflows when appropriate.
- **High-pressure environments**: separate watch-only and signing devices, documented recovery procedures, and minimal cross-device metadata exposure.

## Operational review cadence
Revisit your threat model when you change custody setup, network topology, or operational habits. A control that was enough for a small wallet may be insufficient after you consolidate larger balances or widen your public footprint.

UtxoPocket is watch-only by design, which reduces signing-key risk on mobile. Network metadata and behavioral linkage still matter, so use this model to pick the right daily practices.
