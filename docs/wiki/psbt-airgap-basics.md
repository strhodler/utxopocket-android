---
id: psbt-airgap-basics
title: PSBT air‑gap basics
summary: Draft, transfer, and verify PSBTs with offline signers using QR, microSD, or USB while preserving labels and privacy.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [descriptors-advanced, rbf-cpfp-strategies, fee-selection-playbook]
glossary_refs: [psbt, rbf, cpfp, vbytes]
keywords: [psbt, airgap, signing]
---

## Why it matters
Air‑gapped signing reduces key‑exposure risk. Clean PSBTs make the process reliable: correct inputs, change, fees, and labels transfer between tools without surprises.

## Core guidance
- Include key origins and account metadata so signers can validate scripts and paths.
- Pre‑label inputs/outputs (BIP‑329) before export; labels help you verify intent on the signer.
- Size fees using sats/vbyte and leave room for RBF if urgency may change.
- Transfer over QR or removable media; verify the PSBT on the signer and again on return before broadcast.

## Practical steps
- Draft PSBT: choose inputs within one compartment, avoid toxic change when possible.
- Review: confirm outputs, change script type, and fee rate; compare vbytes estimate.
- Transfer: export via QR or file; import into the signer; approve after on‑device checks.
- Finalize: import signatures, optionally bump via RBF if needed, then broadcast over your own backend.

## Action checklist
- [ ] Inputs come from one compartment; labels present.
- [ ] Fee and vbytes align with policy; RBF considered.
- [ ] Signer validates key origins and paths.
- [ ] Final PSBT re‑checked before broadcast.

