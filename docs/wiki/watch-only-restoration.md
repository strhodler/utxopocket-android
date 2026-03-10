---
id: watch-only-restoration
title: Watch‑only restoration
summary: Restore descriptors, tune discovery, and verify results without exposing private keys.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [encrypted-watch-only-backup, backup-recovery-drill, descriptors-advanced, descriptor-maps-and-recovery]
glossary_refs: [gap-limit, descriptor, encrypted-backup, backup-integrity]
keywords: [watch-only, restore, discovery]
---

## Goals
Recreate monitoring from descriptors alone, scan reliably, and confirm that balances, addresses, and labels match expectations.

## Inputs required
- Output descriptors (with checksums) for each compartment.
- Optional encrypted `.ubak` watch-only backup for faster local rebuild.
- Optional: label export (BIP‑329) and prior account map.
- Access to your own Electrum server over Tor (preferred).

## Steps
1. Choose restore source: descriptor-only restore or encrypted `.ubak` import.
2. If using `.ubak`, unlock preview, verify wallet names/count, then confirm import.
3. If using descriptors, import one bucket at a time and set a conservative gap limit initially (for example 50-100) if historical usage was heavy.
4. Start discovery against your own Electrum server to avoid leaking queries.
5. Verify first/last used addresses, total UTXOs, labels, collections, and wallet detail preferences against your records.
6. Repeat for each descriptor bucket when doing descriptor-only recovery.

Importing a `.ubak` file replaces current local watch-only wallets and related local metadata on that device before rehydration.

## Validation tips
- Cross‑check a few known txids and receive paths.
- Confirm preview metadata before import: created date, wallet names, and wallet count.
- If expected funds are missing, raise the gap limit and rescan.
- Keep a record of final gap limits and node height for reproducibility.

## Action checklist
- [ ] Import descriptors and label exports per bucket.
- [ ] If using `.ubak`, validate preview metadata before confirming import.
- [ ] Run discovery with a safe initial gap limit; adjust as needed.
- [ ] Verify known addresses/transactions plus labels and collections.
- [ ] Store final settings (gap limits, node height) with your descriptor book.
