---
id: encrypted-watch-only-backup
title: Encrypted watch-only backup
summary: Export and import encrypted .ubak files to restore watch-only wallets, labels, collections, and selected preferences.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [backup-recovery-drill, watch-only-restoration, descriptor-maps-and-recovery, label-export-bip329-workflows]
glossary_refs: [watch-only, encrypted-backup, backup-passphrase, backup-integrity, bip-329]
keywords: [backup, ubak, restore, encryption, watch-only]
---

## Why it matters
Encrypted backups reduce recovery friction after device loss, reinstall, or panic wipe while preserving the watch-only model.

## What `.ubak` includes
- Wallet metadata: names, descriptors, networks, colors, sort order.
- Label state: transaction labels, UTXO labels, and pending BIP-329 entries.
- UTXO canvas state: collections, memberships, and canvas items.
- Selected app and wallet detail preferences.

## What `.ubak` excludes
- Private keys, seeds, mnemonic phrases, WIF, and signing blobs.
- PIN and duress PIN material.
- Node endpoint selection and related network-policy state.

## Export flow
1. Open `Settings -> Wallets -> Backups -> Encrypted backup`.
2. Choose **Export .ubak**.
3. Set a strong passphrase and confirm.
4. Store the backup file and passphrase separately.

## Import flow
1. Select a `.ubak` file.
2. Enter the passphrase and decrypt preview.
3. Verify created time, wallet count, and wallet names.
4. Confirm import.

Import replaces current local watch-only wallets and related local metadata on that device before rehydration.

## Integrity and safety checks
- Wrong passphrase and tampered files fail with safe errors.
- Unsupported schema/security parameters are rejected.
- Keep descriptor maps as a second recovery path.

## Action checklist
- [ ] Keep at least one recent `.ubak` plus a separate descriptor map record.
- [ ] Store backup passphrase separately from the backup file.
- [ ] Verify preview metadata before every import.
- [ ] Reconfigure PIN and duress PIN after restore.
