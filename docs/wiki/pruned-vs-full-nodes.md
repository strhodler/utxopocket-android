---
id: pruned-vs-full-nodes
title: Pruned vs full nodes
summary: Compare storage, bandwidth, and verification tradeoffs; understand watch‑only scanning constraints.
category_id: privacy-networking
category_title: Privacy & Networking
category_description: How to protect your privacy and understand how Bitcoin nodes communicate.
related: [node-trust-model, node-connectivity, watch-only-restoration]
glossary_refs: [pruned-node, full-node]
keywords: [nodes, validation, scanning]
---

## Why it matters
Your node choice shapes trust and privacy. Full nodes validate everything; pruned nodes save disk by discarding old blocks after validation.

## Core differences
- Storage: full nodes keep the full chain; pruned nodes keep recent blocks (per configured size).
- Scanning: watch‑only rescans on pruned nodes may be limited to available history or require external indexes.
- Privacy: both should be run behind Tor; avoid public endpoints for wallet queries.

## Practical guidance
- If possible, use a full node for initial imports and heavy rescans.
- For pruned nodes, pair with an Electrum server indexer to support historical queries.
- Monitor disk, mempool, and connectivity; keep software patched.

## Action checklist
- [ ] Choose node type based on resources and needs.
- [ ] Enable Tor and verify onion reachability.
- [ ] Plan rescans/imports accordingly (full vs pruned constraints).
