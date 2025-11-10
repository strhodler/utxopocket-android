---
id: electrum-servers
title: Electrum servers and privacy
summary: Why self‑hosting behind Tor matters, how public servers see your activity, and basic health checks.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [watch-only-restoration, node-connectivity, node-trust-model, tor-hardening-for-nodes]
glossary_refs: [electrum-server]
keywords: [electrum, tor, privacy]
---

## Trust and exposure
Public servers can observe address lookups and timing patterns. Using your own server over Tor keeps queries local and unlinkable.

## Self‑host basics
- Run a server compatible with your node (e.g., electrs) and keep it in sync with the same pruning policy as your wallet expectations.
- Restrict access and route all traffic via Tor.
- Monitor uptime, index health, and disk usage to prevent partial scans.

## Operational checks
- Verify headers/height match your node.
- Confirm address queries return consistent results across restarts.
- Periodically rescan a small descriptor to detect index drift.

## Action checklist
- [ ] Prefer your own Electrum server; avoid random public endpoints.
- [ ] Route wallet queries over Tor.
- [ ] Add basic health checks and alerts for index status and node height.
