---
id: node-connectivity
title: Connect UtxoPocket to your node
summary: Use Tor by default, switch to Local Direct only for trusted private/local nodes, and verify connectivity before importing descriptors.
category_id: privacy-networking
category_title: Privacy & Networking
category_description: How to protect your privacy and understand how Bitcoin nodes communicate.
related: [tor-hardening-for-nodes, electrum-servers, node-trust-model]
glossary_refs: [tor, electrum-server]
keywords: [tor, node, electrum, connectivity, networking]
---

## Connection mode requirements
- Tor is the default mode for bundled presets and custom onion endpoints. Wait until the status bar shows Tor as “Running” before adding or switching nodes in Tor mode.
- Local Direct is optional and explicit opt-in. It accepts only custom private/local IP literals over `tcp://` (IPv4/IPv6 only; no DNS, `.local`, or hostnames).
- If mode prerequisites are not met (for example, Tor unavailable while Tor mode is active), connection requests are blocked fail-closed. UtxoPocket never auto-switches between Tor and Local Direct.

## Adding or switching nodes
- Use `More → Network` to select a bundled public Electrum server or point to your own. Custom entries use onion hostnames in Tor mode, or private/local IP literals in Local Direct mode.
- Switching between Tor and Local Direct always clears the active node selection and leaves connectivity idle. After switching, explicitly activate a compatible node to reconnect.
- The Connections & network screen has no manual `Connect Tor` button. In Tor mode, Tor starts only after you activate a Tor-compatible node.
- Custom-node save validates the Electrum genesis hash for the selected app network and blocks save on mismatch.
- After selecting a node, pull to refresh on the home screen so the wallet replays discovery using the new backend. Expect a short lock while descriptors rescan.

## Connectivity checks
- Compare the reported block height and fee rate in the app bar with another trusted source. Large mismatches indicate a stale or malicious server.
- In Tor mode, renew your Tor identity from the Tor status screen if you suspect the circuit is sluggish or the server is unreachable.
- Keep at least one known-good node per mode configured. If the active host is down, switch peers manually. UtxoPocket does not auto-rotate presets or auto-fallback across modes.
