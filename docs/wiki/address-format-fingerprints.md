---
id: address-format-fingerprints
title: Address format fingerprints
summary: What script types reveal on‑chain (legacy vs SegWit vs Taproot), privacy tradeoffs, and safe defaults for watch‑only users.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [descriptors-advanced, bech32-vs-bech32m, script-type-tradeoffs]
glossary_refs: [bech32, bech32m]
keywords: [script, p2wpkh, taproot, bech32, bech32m]
---

## Why it matters
Script types leave recognizable fingerprints on‑chain (fees, size, and structure). Mixing formats carelessly can cluster your activity. Choosing and sticking to modern, efficient scripts reduces cost and leakage.

## Common script types and signals
- Legacy (P2PKH, base58): Larger size, higher fees, widely recognized; easy to cluster with older patterns.
- P2SH (wrapped): Transitional, sometimes used to embed other scripts; still larger than native SegWit.
- P2WPKH (SegWit v0, bech32): Smaller, cheaper, and widely supported; good default for singlesig wallets.
- P2TR (Taproot, bech32m): Efficient key‑path spends and optional script‑path; best default when supported.

## Privacy and fee tradeoffs
- Mixed formats in one wallet increase fingerprinting risk and can expose wallet migrations or merges.
- Taproot key‑path spends look uniform, but script‑path reveals spending conditions; design policies accordingly.
- Native SegWit (v0 and v1) reduces fees for the same security, lowering pressure to merge inputs.

## Watch‑only guidance
- Standardize on one format per descriptor/bucket to avoid cross‑format merges.
- Prefer P2WPKH or P2TR when supported by your signing flow; avoid legacy unless required for compatibility.
- Label format transitions explicitly (BIP‑329) to audit merges and keep compartments distinct.

## Action checklist
- [ ] Choose a modern default (P2WPKH or P2TR) and stick to it per bucket.
- [ ] Avoid mixing formats in a single spend unless absolutely necessary.
- [ ] Document any format change in labels to prevent accidental merges later.
