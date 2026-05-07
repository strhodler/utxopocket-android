---
id: bitcoin-dev-kit
title: Bitcoin Dev Kit in practice
summary: What BDK provides in a watch-only architecture and how it interacts with descriptors, syncing, and transaction drafting.
category_id: wallet-basics
category_title: Wallet basics
category_description: Core concepts for receiving, tracking, and preparing transactions safely.
related: [descriptors-101, wallet-syncing, psbt-explained, utxopocket-overview]
glossary_refs: [descriptor, psbt, electrum-server]
keywords: [bdk, descriptors, sync]
---

## Role in the stack
BDK handles wallet state logic: script derivation, chain scanning, coin selection primitives, and PSBT construction. App policy decides when and how those capabilities are exposed.

## Why it matters for watch-only
A watch-only app can safely use BDK for monitoring and draft creation without ever loading private keys. Security depends on strict descriptor and policy boundaries around it.

## UtxoPocket alignment
UtxoPocket uses BDK in a watch-only flow with no signing path. Tor is the default transport mode, Local Direct is an explicit option for private/local IP literal custom endpoints, and there is no automatic fallback between modes.

## Action checklist
- [ ] Keep descriptor imports public-only.
- [ ] Validate backend/network compatibility before syncing.
- [ ] Separate drafting from signing devices.
