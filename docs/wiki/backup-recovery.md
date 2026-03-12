---
id: backup-recovery
title: Backup and recovery fundamentals
summary: How to plan, test, and maintain recoverable watch-only backups without weakening privacy posture.
category_id: safety-operations
category_title: Safety and operations
category_description: Operational practices for resilient, private wallet use.
related: [encrypted-watch-only-backup, backup-recovery-drill, watch-only-restoration, operational-hygiene]
glossary_refs: [encrypted-backup, backup-passphrase, backup-integrity, descriptor]
keywords: [backup, recovery, resilience]
---

## Recovery is a process
Backups are useful only if restoration works under pressure. Run scheduled recovery drills and verify descriptor scope, labels, and expected balances.

## Keep scope explicit
Document what your backup includes and excludes. In UtxoPocket, `.ubak` backups cover watch-only descriptors and selected metadata, and do not include signing keys, seeds, or PIN material.

## Failure handling
Treat failed preview, integrity mismatch, or schema rejection as hard stops. Investigate before retrying with different files or passphrases.

## Action checklist
- [ ] Keep at least two encrypted backup copies in separate locations.
- [ ] Test restore flow on a regular schedule.
- [ ] Record recovery steps in a short runbook.
