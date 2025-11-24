# Connection Troubleshooting & Reporting Guide

Use this guide to gather the minimum information needed to debug Tor/Electrum connectivity without exposing wallet data.

## Before Reporting
- Make sure **Network error log** (Settings → Security → Advanced → Network error log) is ON. Logs stay local and are sanitized (masked hosts, no descriptors/txids).
- Note if you selected a **public** or **custom** node, and whether it is **onion** or **clearnet**.
- Confirm your device is online and Tor has finished bootstrapping.

## What to Share (sanitized)
1) **What you were doing**: connect/sync/broadcast/refresh, and the button or screen you used.
2) **Endpoint**: public/custom, onion/clearnet, and port.
3) **Network**: Wi‑Fi/Cell/VPN, and whether captive portal or firewall is possible.
4) **Timing**: when it failed, and if it happens repeatedly or intermittently.
5) **Logs**: open the log viewer (Settings → Security → Advanced → Network error log), tap **Copy logs**, and paste the sanitized text into the issue/chat.

## Quick Checks
- If Tor shows “connecting” for long: toggle airplane mode off/on, ensure no captive portal, then retry.
- If using a custom node: double‑check host/port and whether it requires SSL (`ssl://`) or TCP (`tcp://`).
- Switch between a **public** node and your **custom** node to see if the error is node-specific.

## Privacy Notes
- The copied log contains only masked/hashed hosts, operation type, transport, and error class/message. No descriptors, addresses, or balances are included.
- You can clear the log anytime from the viewer; panic wipe also removes this log.***
