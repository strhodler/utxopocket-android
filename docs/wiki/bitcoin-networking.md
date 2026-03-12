---
id: bitcoin-networking
title: Bitcoin networking essentials
summary: How nodes relay data, why topology affects privacy, and what watch-only operators should verify.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [node-connectivity, electrum-servers, tor-integration, why-tor]
glossary_refs: [full-node, spv, electrum-server, tor]
keywords: [networking, relay, privacy]
---

## Network model
Bitcoin nodes exchange blocks and transactions using peer-to-peer gossip. What your wallet asks, when it asks, and through which transport all affect metadata exposure.

## Practical privacy view
Querying third-party infrastructure can leak cluster hints even if addresses are valid. In UtxoPocket, Tor is the default mode; Local Direct is optional only for private/local IP literal custom endpoints.

## Reliability and correctness
Network partitions, stale indexes, or policy differences can produce temporary inconsistencies. Cross-checking data sources is safer than assuming first response is final.

## Action checklist
- [ ] Prefer self-hosted infrastructure when possible.
- [ ] Route wallet queries through Tor in normal operation.
- [ ] Verify chain height and consistency after outages.
