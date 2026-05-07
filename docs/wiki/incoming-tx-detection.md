---
id: incoming-tx-detection
title: Incoming transaction detection
summary: Lightweight Electrum watcher on the active connection mode for early incoming signals, reconciled by canonical BDK sync.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [electrum-servers, address-discovery-and-gap-limit, watch-only-restoration, watch-only-threat-model]
glossary_refs: [electrum-server, tor, spv, gap-limit]
keywords: [incoming, electrum, polling, tor, watch-only]
---

## Why it matters
Watch-only users still need timely alerts for incoming funds without waiting for a heavy wallet refresh. UtxoPocket runs a lightweight Electrum watcher on the active connection mode (Tor default or optional Local Direct) to detect early incoming signals, then keeps BDK sync as the canonical source for final wallet transaction state.

## Core guidance
- Prefer your own Electrum server in Tor mode when possible; public servers can observe lookup timing and patterns. In Local Direct mode, watcher checks are limited to custom private/local IP literal endpoints and never auto-fallback to Tor.
- The watcher sends an Electrum `server.version` handshake before subscribe/history calls and uses both `listunspent` + `get_history` to detect early tx states.
- Detection states are light-only: `UNCONFIRMED` (mempool seen) and `CONFIRMED_LIGHT` (confirmed height observed by watcher). These states are UX hints, not canonical wallet truth.
- BDK sync remains canonical. Placeholders persist with no time-based expiration and are removed only after a successful BDK sync includes the same txid.
- Watch coverage follows the wallet receive window (current unused addresses constrained by gap/stop-gap policy), not a fixed "current + 5" rule.
- On the Receive screen, the “Check address” button queries the active address via the same active-mode client (Tor or Local Direct); if activity is detected the app advances to the next address to avoid reuse.
- Foreground resumes watcher checks on its configured cadence; background stops watcher checks to respect device limits.

## Action checklist
- [ ] Prefer Tor mode for stronger network privacy; use Local Direct only with trusted private/local infrastructure.
- [ ] Toggle “Incoming transaction detection” in Wallet Settings.
- [ ] After a dialog/placeholder appears, run a wallet sync so canonical BDK history can reconcile and remove matched placeholders.
- [ ] Rotate receiving addresses promptly—use the Receive screen check to confirm usage and advance.
- [ ] Keep your descriptor exports and gap-limit documentation up to date so restores match what the watcher monitors.
