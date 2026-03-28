---
id: tor-integration
title: Tor integration in wallet operations
summary: Why Tor transport matters for Bitcoin metadata and how fail-closed design protects privacy guarantees.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [why-tor, tor-vs-vpn, tor-hardening-for-nodes, electrum-servers]
glossary_refs: [tor, electrum-server, bridge]
keywords: [tor, networking, privacy]
---

## Tor as transport control
Tor helps hide source network metadata from wallet infrastructure and observers. It does not fix on-chain linkage by itself.

## Fail-closed requirement
For strong privacy guarantees, Tor-dependent modes should stop syncing when Tor is unavailable instead of quietly switching to clearnet.

## UtxoPocket behavior
UtxoPocket defaults to Tor mode and aborts sync if Tor proxying is not available. Tor transport is provided by the app-owned embedded runtime (`tor-android` + pinned `jtorctl`) through a foreground service lifecycle.

Local Direct is a separate, explicit mode for private/local IP literal custom endpoints only. Local Direct does not implicitly start Tor, and there is no automatic switching or clearnet fallback between modes.

## Action checklist
- [ ] Verify Tor state before expecting sync.
- [ ] Treat transport failures as blocking, not optional warnings.
- [ ] Pair Tor routing with good on-chain hygiene.
