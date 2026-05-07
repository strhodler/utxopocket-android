# Connection Troubleshooting & Reporting Guide

Use this guide to gather the minimum information needed to debug Electrum connectivity in Tor or Local Direct mode without exposing wallet data.

## Before Reporting
- Make sure **Network error log** (Settings → Security → Advanced → Network error log) is ON. Logs stay local and are sanitized (masked hosts, no descriptors/txids).
- Note your connection mode (**Tor** or **Local Direct**) and node type (**public preset**, **custom onion**, or **custom local IP literal**).
- Confirm your device is online; if using Tor mode, confirm Tor has finished bootstrapping.

## What to Share (sanitized)
1) **What you were doing**: connect/sync/broadcast/refresh, and the button or screen you used.
2) **Endpoint**: public preset, custom onion, or custom local IP literal, plus port.
3) **Network**: Wi‑Fi/Cell/VPN, and whether captive portal or firewall is possible.
4) **Timing**: when it failed, and if it happens repeatedly or intermittently.
5) **Logs**: open the log viewer (Settings → Security → Advanced → Network error log), tap **Copy logs**, and paste the sanitized text into the issue/chat.

## Quick Checks
- If Tor shows “connecting” for long: toggle airplane mode off/on, ensure no captive portal, then retry.
- If using a custom onion node in Tor mode: double-check onion host/port formatting and that Tor is fully bootstrapped.
- If using Local Direct: use only private/local IP literals (no DNS/`.local`/hostnames), and confirm the app network (Mainnet/Testnet3/Testnet4/Signet) matches the node network.
- If custom-node save is blocked, verify the node Electrum genesis hash matches the selected network.
- Switch between a **public** node and your **custom** node to see if the error is node-specific.

## Logcat Signals For Mode-Switch Race Regressions
- Filter tags with `adb logcat -s WalletSyncOrchestrator NodeSyncRunner ConnectionOrchestratorV2`.
- Expected sequence when switching mode without activating a node: `WalletSyncOrchestrator` logs `event=Disconnect`, then `NodeSyncRunner` does not resume sync until a new activation intent.
- Expected sequence after explicit activation: `WalletSyncOrchestrator` logs `event=Start`, then `NodeSyncRunner` progresses through `Node status -> Connecting` and eventually `Node status -> Syncing`/`Node status -> Synced`.
- Red flag: after a disconnect, any late `Connecting`/`Synced` status appears without a fresh `event=Start` for the same network.

## Privacy Notes
- The copied log contains only masked/hashed hosts, operation type, transport, and error class/message. No descriptors, addresses, or balances are included.
- You can clear the log anytime from the viewer; panic wipe also removes this log.
