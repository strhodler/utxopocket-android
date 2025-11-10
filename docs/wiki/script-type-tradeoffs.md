---
id: script-type-tradeoffs
title: Script type tradeoffs
summary: Privacy and efficiency differences among common script types and how mixed scripts leak information.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [address-format-fingerprints, bech32-vs-bech32m, taproot-privacy-model]
glossary_refs: [script]
keywords: [script, p2pkh, p2wpkh, taproot]
---

## Why it matters
Different script types affect fees, size, and on‑chain fingerprints. Mixing them in one spend or bucket can expose migrations, wallet type, and clustering opportunities.

## Tradeoffs at a glance
- P2PKH (legacy): Highest weight and fees; recognizable; least private in mixed environments.
- P2SH (wrapped): Transitional; adds indirection; still heavier than native SegWit.
- P2WPKH (SegWit v0): Efficient, widely supported, good default for singlesig.
- P2TR (Taproot): Efficient key‑path spends; script‑path reveals conditions only when used; strong default when tooling supports it.

## Mixing scripts leaks
- Inputs of different script types in one transaction create distinct fingerprints and suggest wallet merges.
- Output script changes over time in the same cluster hint at upgrades or policy shifts; adversaries track these transitions.

## Watch‑only guidance
- Keep one script family per descriptor/bucket to avoid cross‑format merges.
- Prefer native SegWit (v0/v1). Use legacy only for compatibility with older counterparties.
- Document any script transitions in labels and avoid co‑spends across formats.

## Action checklist
- [ ] Standardize on P2WPKH or P2TR per bucket.
- [ ] Avoid mixing script types in a single spend.
- [ ] Label script transitions; prevent cross‑format consolidation later.
