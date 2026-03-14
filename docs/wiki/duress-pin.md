---
id: duress-pin
title: Duress PIN — fake wallet list mode
summary: Hidden duress unlock shows only a decoy wallet list, blocks detail navigation, and hides connections UI while Tor/node stay disconnected.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [watch-only-threat-model, node-connectivity, incoming-tx-detection]
glossary_refs: [tor, watch-only]
keywords: [duress, pin, fake-wallet, tor, electrum]
---

## Why it matters
If you must unlock under pressure, revealing a believable but inert view protects real wallet data and network posture. A list-only decoy avoids touching wallet detail flows while hiding connectivity affordances that might expose real nodes or Tor status.

## Core behavior
- **Activation**: Entering the duress PIN activates an in-memory `FakeActive` session and pops the backstack to the wallet list. No state is written beyond the duress config itself.
- **Decoy list**: Shows a single fake wallet with a fixed balance (default 50,000 sats). Cards look normal but ignore taps; deep links to wallet detail are blocked/redirected to the list.
- **Connections hidden**: Top-bar node icon is hidden. The home connections banner keeps layout but hides its icon and ignores taps. Node status routes and prompts are blocked.
- **Networking paused**: Tor/node disconnect; sync polling and observers pause while duress is active. Incoming TX badge/sheet is suppressed.
- **Zero tells**: No snackbars/toasts indicate duress; PIN prompts treat duress as a normal success. Fake state clears on restore or app restart; only duress config persists.
- **Calculator camouflage compatibility**: If the optional calculator camouflage is enabled, it may appear before PIN entry, but duress unlock semantics stay the same once the PIN step is reached.

## Enabling the duress PIN
1) Open Settings → Security (requires the normal PIN).
2) Rapid-tap the normal PIN list item N times (default 7) until the duress toggle appears (countdown snackbars on taps 5/6/7).
3) Toggle duress on, enter the normal PIN, then set the duress PIN twice. The toggle hides again on exit/timeout.

## Using duress
- At any lock prompt, enter the duress PIN. The app unlocks, jumps to the wallet list, and switches to the decoy state.
- Wallet cards remain non-interactive; node/connections entry points do nothing; incoming TX UI is silent; Tor/node stay disconnected.

## Exiting duress
1) Go to Settings → Security with the normal PIN.
2) Repeat the tap gesture to reveal the duress toggle.
3) Toggle off (normal PIN required). This restores the real session, resumes Tor/node, and re-enables normal UI.
4) Clearing the normal PIN also clears the duress config.

## QA checklist
- Unlock with duress PIN → backstack pops to wallet list; card untappable but not visually disabled.
- Top-bar node icon hidden; home connections banner shows layout but no icon/action.
- Node status/banners/prompts do not navigate; incoming badge/sheet suppressed.
- Tor/node disconnected; no sync or observer churn during duress.
- Rotation/recreation keeps duress gating (still on list, card untappable, connections hidden).
- Restore with normal PIN/toggle → real wallets and connections UI return; incoming sheet works.
