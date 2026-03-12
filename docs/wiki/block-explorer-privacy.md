---
id: block-explorer-privacy
title: Block Explorer Privacy
summary: Reduce lookup leakage by preferring self-hosted explorers and routing explorer sessions through Tor.
category_id: privacy-networking
category_title: Privacy & Networking
category_description: How to protect your privacy and understand how Bitcoin nodes communicate.
related: [privacy-threat-models, node-connectivity, node-trust-model]
glossary_refs: [tor, electrum-server, watch-only]
keywords: [block explorer, tor, privacy, metadata, watch-only]
---

## What explorers can learn
Public explorers can log your IP, browser fingerprint, timestamps, and every txid or address you query. Repeated lookups from the same network identity make clustering easier, especially when those lookups overlap with addresses your wallet monitors.

## Preferred setup
- Run your own explorer stack (for example, mempool + your node) and access it over Tor or a trusted private network.
- If you use third-party explorers, open links in Tor Browser so wallet-related browsing does not mix with your normal browser identity.
- Avoid switching between many explorers in the same troubleshooting session unless you need redundancy; each additional endpoint sees more of your query graph.

## In-app link hygiene
When configuring a custom block explorer in UtxoPocket, include the full transaction path pattern expected by the app so txids are appended correctly and you avoid manual copy/paste between apps.

Keep explorer use as a diagnostic tool, not a constant workflow. For canonical wallet state, trust your descriptor sync backend and your own infrastructure.
