---
id: pruned-node
title: Pruned node
summary: A full‑validating node that discards old blocks after verification to save disk space; scanning depth can be limited.
related: [node-trust-model]
keywords: [validation, storage]
---

A pruned node validates the entire chain but keeps only a recent window of block data on disk. It offers the same consensus guarantees as a full‑archive node with a much smaller storage footprint. Deep historical rescans may require external block data or reindexing, so plan discovery ranges and restores accordingly in watch‑only setups.

