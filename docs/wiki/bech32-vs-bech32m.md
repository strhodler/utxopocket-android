---
id: bech32-vs-bech32m
title: Bech32 vs Bech32m
summary: Understand SegWit address encodings and how they map to script types for safe receiving and monitoring.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [address-format-fingerprints, script-type-tradeoffs]
glossary_refs: [bech32, bech32m]
keywords: [addresses, segwit, taproot]
---

## Why it matters
Bech32 and Bech32m look similar but map to different script families. Using the right format prevents failed payments and avoids leaking wallet internals.

## Core guidance
- Bech32 encodes SegWit v0 (e.g., P2WPKH/P2WSH). Bech32m encodes SegWit v1 (Taproot) and later versions.
- Do not mix formats within one compartment if script uniformity matters for privacy.
- Verify derived receive addresses against your descriptor to catch path or script mismatches.

## Practical checks
- Scan a known receive address with a tool that identifies script type; confirm it matches your descriptor’s intent.
- If moving to Taproot, update policies and labels; don’t partially migrate one compartment.

## Action checklist
- [ ] Use Bech32 for v0, Bech32m for v1 (Taproot).
- [ ] Keep script families consistent within a compartment.
- [ ] Verify address derivations after imports.

