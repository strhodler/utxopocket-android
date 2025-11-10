---
id: tor-hardening-for-nodes
title: Tor hardening for nodes
summary: Keep wallet‑to‑node traffic private with Tor bridges and sane network hygiene in restrictive environments.
category_id: privacy-networking
category_title: Privacy & Networking
category_description: How to protect your privacy and understand how Bitcoin nodes communicate.
related: [node-connectivity, node-trust-model, electrum-servers]
glossary_refs: [tor, bridge, electrum-server]
keywords: [tor, bridges, censorship]
---

## Why it matters
If your network censors Tor or Bitcoin traffic, your wallet’s queries can leak sensitive metadata. Hardening keeps connectivity private and reliable.

## Core guidance
- Prefer your own backend reachable via Tor; avoid rotating through public servers.
- Configure Tor bridges or pluggable transports when direct access is blocked.
- Isolate wallet traffic from other apps; avoid DNS leaks and clear‑net fallbacks.
- Monitor connectivity and fail closed if Tor is unavailable.

## Practical steps
- Obtain bridge lines from trusted sources; configure them on your node host.
- Verify onion reachability from the device running UtxoPocket before importing descriptors.
- Rotate bridges periodically and review logs for bootstrap failures.

## Action checklist
- [ ] Backend reachable over Tor; no clear‑net fallback.
- [ ] Bridges configured and tested where required.
- [ ] Logs reviewed for bootstrap errors and retries.

