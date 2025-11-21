---
id: node-connectivity
title: Connect UtxoPocket to your node
summary: Keep Tor running, choose a healthy backend, and verify connectivity before importing descriptors.
category_id: privacy-networking
category_title: Privacy & Networking
category_description: How to protect your privacy and understand how Bitcoin nodes communicate.
related: [why-tor, electrum-servers, node-trust-model]
glossary_refs: [tor, electrum-server]
keywords: [tor, node, electrum, connectivity, networking]
---

## Tor-first requirements
- UtxoPocket routes every RPC and Electrum call through its embedded Tor daemon. Wait until the status bar shows Tor as “Running” before attempting to add or switch nodes—otherwise connection requests are blocked.
- If your device is offline or Tor cannot bootstrap, the node picker disables custom endpoints. Fix Tor first (renew identity, toggle airplane mode, or check captive portals) and then retry.
- Keep onion endpoints handy. Clearnet hosts defeat the privacy guarantees promised in the README and are now rejected entirely.

## Adding or switching nodes
- Use `More → Network` to select a bundled public Electrum server or point to your own. Custom entries need the onion hostname and port; Tor handles the transport so no SSL toggle is exposed.
- The wallet refuses to add a node when Tor is offline. This prevents accidental clearnet leaks and keeps behavior consistent across devices.
- After selecting a node, pull to refresh on the home screen so the wallet replays discovery using the new backend. Expect a short lock while descriptors rescan.

## Health checks
- Compare the reported block height and fee rate in the app bar with another trusted source. Large mismatches indicate a stale or malicious server.
- Renew your Tor identity from the Tor status screen if you suspect the circuit is sluggish or the server is unreachable.
- Keep at least one fallback server configured. If the primary onion host is down, switching to a known-good peer keeps monitoring uninterrupted.
