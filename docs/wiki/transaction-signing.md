---
id: transaction-signing
title: Transaction signing workflow
summary: A secure signing lifecycle from watch-only draft to final broadcast with independent verification steps.
category_id: safety-operations
category_title: Safety and operations
category_description: Operational practices for resilient, private wallet use.
related: [psbt-explained, keys-and-seeds, spending-policies, watch-only-threat-model]
glossary_refs: [psbt, seed-phrase, key-fingerprint, watch-only]
keywords: [signing, workflow, psbt]
---

## Separation is the control
Draft and verify on a networked watch-only device, then sign on an isolated signer. This reduces key exposure while preserving review visibility.

## Verification checkpoints
- Confirm recipient and amount.
- Confirm fee and change destination policy.
- Confirm signer identity and expected policy template.

## Post-signing hygiene
Store minimal audit context and confirm broadcast status from at least one independent source.

## Action checklist
- [ ] Never move seed material to the monitoring phone.
- [ ] Require explicit output and fee review before signing.
- [ ] Keep signer firmware and policy docs current.
