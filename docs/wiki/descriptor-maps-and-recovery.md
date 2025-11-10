---
id: descriptor-maps-and-recovery
title: Descriptor maps and recovery
summary: Maintain descriptor bundles (origins, checksums, account maps, gap limits) so watch‑only restores are reproducible across backends.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [descriptors-advanced, watch-only-restoration, bip389-multipath-practical]
glossary_refs: [descriptor, descriptor-checksum]
keywords: [descriptor, recovery, gap-limit]
---

## Why it matters
Reproducible monitoring depends on restoring the exact same descriptor set with correct origins and discovery settings. Small mismatches lead to missing history, phantom balances, or privacy leaks during rescan.

## What to store
- Descriptors with checksums: Refuse imports when checksum fails.
- Key origins: Fingerprints and derivation paths for each key.
- Account/branch map: Purpose, coin type, account, external/internal branches actually in use.
- Gap limits: Document intended discovery ranges per branch.

## Restore and verify
- Import the full bundle, verify checksums, and set gap limits explicitly.
- Run a bounded rescan sized to your usage; sample derived addresses to confirm coverage.
- Compare balances and recent history with your prior backend before trusting the new one.

## Operational tips
- Keep descriptor bundles versioned and encrypted at rest.
- When adding historic descriptors, schedule deeper rescans during low‑load windows.
- Log restore time, heights, and any anomalies for future audits.

## Action checklist
- [ ] Store descriptors + checksums + origins + account map + gap limits.
- [ ] Verify checksums on import; refuse mismatches.
- [ ] Rescan within documented ranges; compare against previous backend.
- [ ] Version and protect backups; record restore metadata.
