---
id: multisig-portability
title: Multisig portability
summary: Make multisig wallets reproducible across tools with descriptors, cosigner maps, and PSBT‑first workflows.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [descriptors-advanced, descriptor-maps-and-recovery, psbt-airgap-basics]
glossary_refs: [multisig, cosigner, policy-descriptor, key-origin, key-fingerprint, psbt]
keywords: [multisig, descriptors, cosigners]
---

## Why it matters
Teams and families often use multisig for resilience. Portability prevents lock‑in: any compatible wallet should reproduce the same addresses, validate policies, and sign PSBTs reliably.

## Core guidance
- Use descriptors with `sortedmulti(...)` or policy descriptors; include key origins for each cosigner.
- Maintain a cosigner map (fingerprint → xpub, account path, device label) and share it securely.
- Prefer PSBT workflows; avoid ad‑hoc transaction exports that drop origins or labels.
- Test restores on at least two tools before funding; verify both external and change branches.

## Recovery drills
- Periodically simulate a cosigner loss: can you re‑derive the wallet with quorum‑1?
- Validate that each device refuses to sign for unknown origins or wrong paths.
- Keep an updated inventory of device firmware and backup procedures.

## Action checklist
- [ ] Export/import descriptors with key origins for all cosigners.
- [ ] Verify derived addresses match across two tools.
- [ ] Sign a PSBT round‑trip from each device.
- [ ] Store the cosigner map offline with recovery notes.

