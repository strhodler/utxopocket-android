---
id: self-custody-hygiene
title: Self‑custody hygiene
summary: Practical habits to protect seeds, passphrases, backups, and metadata while keeping your watch‑only monitoring accurate.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [encrypted-watch-only-backup, backup-recovery-drill, watch-only-restoration, label-export-bip329-workflows]
glossary_refs: [seed-phrase, passphrase, backup-passphrase, encrypted-backup, tor]
keywords: [backup, passphrase, tor, labels]
---

## Why it matters
If you leak your seed or passphrase, you lose the coins. If you lose labels or descriptor maps, you lose context and may spend badly. Self‑custody isn’t just key storage—it’s a set of habits that preserve both safety and privacy over time.

## Core habits
1. **Separate the secrets** — Store your seed phrase and passphrase in distinct places; never write them on the same card or photo.
2. **Prefer encrypted backups** — Use UtxoPocket encrypted `.ubak` files to preserve watch-only descriptors, labels, collections, and selected preferences.
3. **Test recovery** — Practice a dry restore on a spare device: verify addresses match and labels are intact before relying on the setup.
4. **Rotate exposure** — If a seed/passphrase was sighted or handled poorly, rotate to a new wallet and move funds methodically.
5. **Verify Tor-only connectivity** — Ensure wallet sync and backend access stay behind Tor and fail closed when Tor is unavailable.

Backup scope reminder: `.ubak` files do not include seed phrases, private keys, PIN, or duress PIN material. Reconfigure PIN and duress PIN after import.

## Labeling and metadata
- Commit to a simple schema (source, intent, date). Consistency beats complexity.
- Label at the moment of receipt/spend; retrofitting later is error‑prone.
- Keep a private “descriptor book”: account fingerprints, derivation paths, policy thresholds, cosigner xpubs.

## Action checklist
- [ ] Create an encrypted `.ubak` backup and store its passphrase separately.
- [ ] Verify a full restore on a secondary device before storing significant value.
- [ ] Store seed and passphrase separately; record when/where each was created.
- [ ] Re-run a backup recovery drill after major descriptor/label reorganizations.
- [ ] Verify your wallet remains in Tor-only mode for chain data access.
