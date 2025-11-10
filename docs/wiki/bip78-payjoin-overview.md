---
id: bip78-payjoin-overview
title: PayJoin (BIP‑78) overview
summary: How interactive input contribution breaks single‑owner assumptions and what watch‑only users can prepare.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [rbf-cpfp-strategies, utxo-selection-heuristics, policy-risk-register]
glossary_refs: [payjoin, cioh]
keywords: [payjoin, bip78, privacy]
---

## Why it matters
PayJoin improves privacy by having the receiver add inputs, defeating the Common‑Input Ownership Heuristic. Even watch‑only users benefit when preparing clean inputs and labels for signers.

## Core concepts
- Interactive negotiation between sender and receiver; the final transaction differs from the initial proposal.
- Receiver inputs make ownership ambiguous; amounts and change patterns become less revealing.
- Requires compatible infrastructure; fall back gracefully when peers are not capable.

## Watch‑only preparation
- Label candidate inputs per intent; pre‑select within one compartment.
- Ensure fee policy allows modest variation; RBF can help if negotiation changes size.
- Export PSBTs with full key origins and metadata for signers.

## Action checklist
- [ ] Prepare inputs within one compartment and label them.
- [ ] Allow small fee variance; consider RBF.
- [ ] Verify final transaction matches intent before broadcast.
