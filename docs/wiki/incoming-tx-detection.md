---
id: incoming-tx-detection
title: Incoming transaction detection
summary: Lightweight Electrum polling over Tor with placeholders and user-controlled refresh.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [electrum-servers, address-discovery-and-gap-limit, watch-only-restoration, watch-only-threat-model]
glossary_refs: [electrum-server, tor, spv, gap-limit]
keywords: [incoming, electrum, polling, tor, watch-only]
---

## Why it matters
Watch-only users still need timely alerts for incoming funds without running a heavy sync. A lightweight Electrum client can poll scripthashes for unconfirmed arrivals, but it must stay Tor-only to avoid leaking address scans and respect gap limits so future addresses aren’t missed. The app now surfaces these signals with placeholders instead of forcing a full wallet refresh that could hurt UX on slow devices.

## Core guidance
- Use your own Electrum server over Tor when possible; public servers can observe lookup timing and patterns. UtxoPocket keeps hostnames unresolved and requires a Tor SOCKS proxy for all polling—if Tor isn’t ready, polling is skipped rather than falling back to clearnet.
- Enable “Incoming transaction detection” in Wallet Settings and pick a polling interval that matches your tolerance for delays vs. battery usage; the slider controls seconds between checks while the app is in foreground.
- The watcher probes the current external address plus the next five gap-limit slots to catch activity on recently shared addresses; keep descriptors and gap settings aligned with your restore expectations.
- When a new txid appears, the app shows a global dialog with a refresh CTA and adds a pending-style placeholder row in the wallet timeline with a clock icon and amount (if known). It does **not** auto-refresh the wallet to avoid heavy downloads on large histories; use the refresh action when convenient.
- On the Receive screen, the “Check address” button queries the active address via the same Tor-only client; if activity is detected the app advances to the next address to avoid reuse.
- Foreground resumes re-arm polling with the chosen interval; background stops polling to respect device limits.

## Action checklist
- [ ] Run your own Electrum server or pick a trusted Tor-only endpoint; avoid clearnet.
- [ ] Toggle “Incoming transaction detection” on and set a sane interval for your device/network.
- [ ] After a dialog/placeholder appears, refresh the wallet when you have bandwidth to record the real transaction.
- [ ] Rotate receiving addresses promptly—use the Receive screen check to confirm usage and advance.
- [ ] Keep your descriptor exports and gap-limit documentation up to date so restores match what the watcher monitors.
