---
id: utxopocket-overview
title: UtxoPocket overview
summary: Product behavior and guarantees: watch-only architecture, Tor-default networking, and no telemetry by default.
category_id: contributor-guides
category_title: Contributor guides
category_description: Practical onboarding topics for testing, debugging, and validating wallet behavior.
related: [watch-only-threat-model, tor-integration, wallet-syncing, backup-recovery]
glossary_refs: [watch-only, tor, descriptor, encrypted-backup]
keywords: [utxopocket, watch-only, tor]
---

## Product posture
UtxoPocket is a watch-only Android wallet. It accepts public descriptors, monitors balances, and prepares transaction context without storing signing keys.

## Networking model
Tor is the default transport mode. Local Direct is optional and explicit for private/local IP literal custom endpoints. In Tor mode, sync fails closed when Tor is unavailable, with no silent fallback.

## Data and privacy
The app ships without analytics, crash-reporting SDKs, or ad SDKs by default. Security controls focus on encrypted local storage, explicit backup scope, and reliable panic wipe behavior.

## Action checklist
- [ ] Keep watch-only boundaries intact in all feature work.
- [ ] Preserve Tor fail-closed behavior.
- [ ] Validate docs whenever posture changes.
