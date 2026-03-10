---
id: encrypted-backup
title: Encrypted backup
summary: A passphrase-protected backup file that stores recoverable watch-only metadata without exposing signing secrets.
related: [encrypted-watch-only-backup, backup-recovery-drill, self-custody-hygiene]
keywords: [backup, encryption, recovery]
---

In UtxoPocket, an encrypted backup is a `.ubak` file used to restore watch-only wallets and local metadata. It requires a backup passphrase and is rejected if tampered with or decrypted using the wrong passphrase.
