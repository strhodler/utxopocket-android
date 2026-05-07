---
id: wallet-syncing
title: Wallet syncing behavior
summary: How descriptor discovery, backend state, and transport policy affect synchronization accuracy.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [address-discovery-and-gap-limit, watch-only-restoration, bitcoin-dev-kit, tor-integration]
glossary_refs: [gap-limit, electrum-server, full-rescan, tor]
keywords: [sync, discovery, electrum]
---

## What sync does
Sync derives script targets from descriptors, queries backend history, and reconciles local wallet state. Correct descriptors and reliable transport are both required.

## Failure modes
Wrong path metadata, stale backend index, or transport interruption can produce partial views. Failures should be explicit so operators can react safely.

## UtxoPocket notes
Tor mode requires an active Tor proxy; otherwise sync aborts. Local Direct is a separate manual mode for private/local IP literal custom endpoints, and there is no automatic transport switching.

## Action checklist
- [ ] Verify descriptor metadata before first sync.
- [ ] Monitor backend height and consistency.
- [ ] Trigger controlled rescan when state looks incomplete.
