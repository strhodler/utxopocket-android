---
id: descriptors-101
title: Descriptors 101
summary: Intro to output descriptors, checksums, and branch structure for reliable watch-only imports.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [descriptors-advanced, descriptor-maps-and-recovery, watch-only-restoration, hd-derivation]
glossary_refs: [descriptor, descriptor-checksum, key-origin, derivation-path]
keywords: [descriptors, wallet import, recovery]
---

## What descriptors solve
Descriptors provide a precise, machine-readable map of scripts, keys, and derivation paths. They replace guesswork with explicit wallet definitions.

## Core parts
- Script template (`wpkh`, `tr`, `wsh(sortedmulti(...))`, and others).
- Key material with origin metadata.
- Derivation branches for external and change addresses.
- Checksum to detect copy errors.

## Watch-only best practice
Import descriptors as explicit pairs or multipath form, verify checksums, and test-derive sample addresses before trusting balances.

## Action checklist
- [ ] Verify descriptor checksum at import time.
- [ ] Confirm network and derivation origins.
- [ ] Keep descriptor maps versioned for recovery.
