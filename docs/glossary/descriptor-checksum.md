---
id: descriptor-checksum
title: Descriptor checksum
summary: Short suffix appended to a descriptor that detects typos and accidental edits.
related: [descriptors-advanced, watch-only-restoration]
keywords: [descriptor, integrity]
---

A checksum is computed from a descriptor’s contents and appended as a suffix (e.g., `#abcd1234`). Wallets verify it on import to ensure the descriptor was not altered. Storing and checking checksums makes watch‑only restores reproducible and guards against silent mismatches.

