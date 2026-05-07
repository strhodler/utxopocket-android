---
id: testnet-regtest
title: Testnet vs regtest
summary: Choose the right testing network for reproducibility, realism, and debugging speed.
category_id: contributor-guides
category_title: Contributor guides
category_description: Practical onboarding topics for testing, debugging, and validating wallet behavior.
related: [testnet-faucets, wallet-syncing, transaction-standardness-vs-consensus, utxopocket-overview]
glossary_refs: [full-node, confirmation, mempool]
keywords: [testnet, regtest, qa]
---

## Two useful environments
Testnet offers shared, public conditions with noisy mempool dynamics. Regtest offers isolated, deterministic control where you mine blocks on demand.

## Selection guidance
Use regtest for repeatable edge cases and CI-like checks. Use testnet for integration behavior with realistic network variance.

## Workflow pattern
Start with regtest to prove logic, then confirm with testnet for interoperability and timing behavior.

## Action checklist
- [ ] Reproduce bugs first on regtest when possible.
- [ ] Validate final behavior on testnet before release.
- [ ] Keep environment assumptions documented in test notes.
