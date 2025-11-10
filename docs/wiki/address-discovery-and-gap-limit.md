---
id: address-discovery-and-gap-limit
title: Address discovery and gap limit
summary: Tune discovery depth for reliable watch‑only scans without wasting resources; understand tradeoffs and validation steps.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [watch-only-restoration, descriptors-advanced]
glossary_refs: [gap-limit, descriptor, electrum-server]
keywords: [discovery, scanning, watch-only]
---

## Why it matters
Watch‑only wallets rely on descriptors and discovery to find funds. If your gap limit is too small, you miss UTXOs; too large and you waste time and leak usage patterns to remote backends you don’t control.

## Core guidance
- Start with conservative defaults (e.g., 20 external / 6 change) and only increase when you have evidence of deeper usage.
- Prefer scanning against your own backend over Tor; public servers observe access patterns.
- Record chosen limits alongside descriptors in your “descriptor map” so restores are reproducible.
- After imports, verify that derived next addresses match another tool using the same descriptor and origin data.

## Validation steps
- Derive N external and M change addresses and confirm they match a reference wallet.
- Cross‑check balances for several historical receive addresses and ensure transactions appear.
- If funds are missing, increase the gap limit incrementally and rescan.

## Action checklist
- [ ] Import descriptors and set initial gap limits.
- [ ] Verify derived addresses match a second tool.
- [ ] Rescan if balances or history look incomplete.
- [ ] Update your descriptor map with the final limits.

