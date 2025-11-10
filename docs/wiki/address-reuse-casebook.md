---
id: address-reuse-casebook
title: Address reuse casebook
summary: Real‑world failures caused by address reuse and the simple habits that prevent them.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [change-output-hygiene, utxo-segregation-playbook, address-format-fingerprints]
glossary_refs: [address-reuse, utxo]
keywords: [privacy, address reuse]
---

## Why it matters
Reusing addresses ties multiple payments to one identity, simplifies clustering, and can reveal income patterns.

## Case studies
- Donation pages that publish a static address lead to public income tracing.
- Merchant refunds to a reused address link customers and operational wallets.
- Personal wallets that reuse change addresses create peel chains.

## Prevention
- Always use fresh receive addresses; verify derivation against your descriptor map.
- Quarantine change; never hand out change addresses publicly.
- Label provenance and intent to avoid accidental co‑spends.

## Action checklist
- [ ] Rotate addresses per payment.
- [ ] Keep compartments separate; avoid merges.
- [ ] Audit labels and history for any accidental reuse.

