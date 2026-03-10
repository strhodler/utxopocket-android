---
id: backup-recovery-drill
title: Backup recovery drill
summary: Run a repeatable dry-run to verify encrypted watch-only backups restore correctly before an emergency.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [encrypted-watch-only-backup, watch-only-restoration, descriptor-maps-and-recovery, self-custody-hygiene]
glossary_refs: [encrypted-backup, backup-passphrase, backup-integrity, descriptor-checksum]
keywords: [recovery, drill, backup, validation]
---

## Goal
Prove that your encrypted backup and passphrase work end-to-end before you need them in production.

## Drill setup
- Use a secondary device or disposable local state.
- Prepare a recent `.ubak`, backup passphrase, and descriptor map notes.
- Keep the signed wallet or source records nearby for verification.

## Drill steps
1. Export a fresh `.ubak` file.
2. Start from clean local state.
3. Import the backup and unlock preview with passphrase.
4. Verify preview metadata (wallet count and wallet names).
5. Confirm import and run sync.
6. Compare known txids, UTXOs, labels, collections, and wallet detail filters with reference records.

## Pass criteria
- Passphrase unlock succeeds and preview loads.
- Import completes without descriptor validation failures.
- Restored watch-only state matches the expected records.

## Failure handling
- If preview metadata mismatches, stop and verify file source.
- If passphrase fails, rotate to a new backup and re-test.
- If state diverges, restore from descriptor map and investigate before trusting balances.

## Action checklist
- [ ] Run a recovery drill at least quarterly.
- [ ] Re-run drills after major descriptor or labeling changes.
- [ ] Record drill date, environment, and validation results.
- [ ] Keep only current backups and securely remove obsolete copies.
