---
id: psbt-explained
title: PSBT explained
summary: How Partially Signed Bitcoin Transactions coordinate drafts, reviews, and signatures across separated devices.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [psbt-airgap-basics, transaction-signing, coin-control, bitcoin-dev-kit]
glossary_refs: [psbt, key-origin, key-fingerprint]
keywords: [psbt, signing flow, review]
---

## Why PSBT exists
PSBT is a transport format for transaction intent and signing metadata. It lets one system build and another system sign without sharing private keys.

## Workflow stages
- Draft inputs/outputs and fee policy.
- Review policy and destination details.
- Sign on isolated signer(s).
- Finalize and broadcast from a networked system.

## Watch-only role
Watch-only wallets are ideal drafters and reviewers because they carry no signing keys. They can enforce structure and highlight suspicious changes before signing.

## Action checklist
- [ ] Review outputs, fee, and change destination before export.
- [ ] Verify signer fingerprints and expected policy.
- [ ] Archive finalized PSBT context for audits.
