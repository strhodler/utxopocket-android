---
id: watch-only-threat-model
title: Watch‑only threat model
summary: What watch‑only protects and what it doesn’t; assumptions, safe defaults, and checks for healthy monitoring.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [node-trust-model, descriptors-advanced, watch-only-restoration]
glossary_refs: [watch-only, full-node, electrum-server, tor]
keywords: [threat model, watch-only, node]
---

## Why it matters
Watch‑only reduces risk by separating monitoring from signing. But it does not protect against all threats: a compromised device, malicious server, or poisoned data can still mislead you. Make the operating assumptions explicit.

## Core assumptions
- No private keys on the device; descriptors/xpubs only.
- You control the backend or connect over Tor to minimize metadata leaks.
- Descriptor maps (origins, checksums, gap limits) are correct and versioned.

## What it protects
- Key exfiltration from the monitoring device.
- Accidental signing on a hot device (no keys present).

## What it does not protect
- Device compromise showing fake balances or receive addresses.
- Untrusted public servers observing your discovery and history queries.
- Configuration mistakes (wrong origin/path) causing missed funds.

## Safe defaults
- Run your own full node or your own Electrum server behind Tor.
- Verify descriptor checksums and origins when importing; test‑derive addresses.
- Keep compartments separate and label intent; audit history after imports.

## Action checklist
- [ ] Monitor via watch‑only; sign on separate hardware.
- [ ] Use your own backend over Tor.
- [ ] Verify descriptors (checksum, origins, gap limits) on import.
- [ ] Cross‑check balances/addresses with a second tool periodically.

