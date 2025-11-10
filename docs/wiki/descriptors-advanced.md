---
id: descriptors-advanced
title: Descriptors — advanced topics
summary: Checksums, origins, account maps, and multipath gotchas to keep watch‑only restores reproducible and consistent across backends.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [watch-only-restoration, descriptor-maps-and-recovery, bip389-multipath-practical]
glossary_refs: [descriptor, descriptor-checksum]
keywords: [descriptor, checksum, origin, multipath]
---

## Why it matters
Descriptors encode how to derive and validate addresses without private keys. Advanced features like checksums and account maps ensure that watch‑only restores produce the same address space across devices and backends.

## Checksums
- Always store and verify descriptor checksums. They detect typos and accidental edits.
- Refuse restores if the checksum does not match; correct at the source instead of guessing.

## Key origins and account maps
- Record key origin info (fingerprint and derivation path) for each key material.
- Maintain an account map that lists purpose, coin type, account, and branch paths you actually use. This keeps discovery bounded and reproducible.

## Multipath basics (BIP‑389)
- Multipath descriptors combine branches (e.g., external/internal) or key paths into a single descriptor.
- Be consistent with branch order and semantics across exports; mismatches lead to missing or duplicated address ranges.
- Validate derived samples after import to confirm path ordering and script variants are interpreted as intended.

## Policy descriptors and miniscript
- Policy/miniscript descriptors can express conditions compactly, but uncommon scripts may hit relay policy edges or tooling gaps.
- Prefer well‑supported templates for spends you expect to broadcast soon; keep complex policies in testable sandboxes first.

## Recovery notes
- Keep a canonical descriptor bundle: descriptors + checksums + key origin map + intended gap limits.
- After restore, run a bounded rescan and compare derived samples and balances with the previous backend before switching.

## Action checklist
- [ ] Store descriptors with checksums and verify them on import.
- [ ] Preserve key origin data and an explicit account/branch map.
- [ ] Validate multipath ordering by sampling derived addresses after import.
- [ ] Rescan within documented gap limits and compare results before trusting.
