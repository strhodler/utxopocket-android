---
id: watch-only-restoration
title: Watch‑only restoration
summary: Restore descriptors, tune discovery, and verify results without exposing private keys.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, descriptors-advanced, descriptor-maps-and-recovery, bip389-multipath-practical]
glossary_refs: [gap-limit, descriptor, electrum-server]
keywords: [watch-only, restore, discovery]
---

## Goals
Recreate monitoring from descriptors alone, scan reliably, and confirm that balances, addresses, and labels match expectations.

## Inputs required
- Output descriptors (with checksums) for each compartment.
- Optional: label export (BIP‑329) and prior account map.
- Access to your own Electrum server over Tor (preferred).

## Steps
1. Import descriptors for one bucket at a time.
2. Set a conservative gap limit initially (e.g., 50–100) if historical usage was heavy; lower later if scans are slow.
3. Start discovery against your own Electrum server to avoid leaking queries.
4. Verify: first/last used addresses, total UTXOs, and labels (if imported) match your records.
5. Repeat for each descriptor bucket.

## Validation tips
- Cross‑check a few known txids and receive paths.
- If expected funds are missing, raise the gap limit and rescan.
- Keep a record of final gap limits and node height for reproducibility.

## Action checklist
- [ ] Import descriptors and label exports per bucket.
- [ ] Run discovery with a safe initial gap limit; adjust as needed.
- [ ] Verify a sample of known addresses and transactions.
- [ ] Store final settings (gap limits, node height) with your descriptor book.
