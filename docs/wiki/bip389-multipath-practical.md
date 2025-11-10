---
id: bip389-multipath-practical
title: BIP‑389 multipath — practical tips
summary: Express external and change branches in one descriptor safely; avoid ordering pitfalls and missed funds.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [descriptors-advanced, watch-only-restoration]
glossary_refs: [multipath, descriptor, gap-limit]
keywords: [bip389, multipath, descriptors]
---

## Why it matters
Multipath descriptors bundle external and change branches (`/<0;1>/*`) into one definition. Done right, they simplify imports; done wrong, they swap branches or miss funds.

## Core guidance
- Keep branch order canonical: external first `0`, change second `1`.
- Use the exact syntax `/<0;1>/*` and verify that both branches derive as expected in a reference tool.
- Pair multipath with explicit gap‑limit choices and record them with your descriptor map.
- Avoid mixing script types within the same account; it complicates scanning and labeling.

## Practical validation
- Derive several addresses from both branches and compare with a known‑good wallet.
- Import, run discovery, and confirm historical transactions appear on both branches.
- Export a small PSBT spending from each branch to validate change return paths.

## Action checklist
- [ ] Verify branch order and derivations match a reference.
- [ ] Set and document gap limits for both branches.
- [ ] Test a draft spend from each branch.

