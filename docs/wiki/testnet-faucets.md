---
id: testnet-faucets
title: Testnet faucets and safe testing
summary: How to obtain test coins responsibly and run realistic wallet drills without polluting production habits.
category_id: contributor-guides
category_title: Contributor guides
category_description: Practical onboarding topics for testing, debugging, and validating wallet behavior.
related: [testnet-regtest, wallet-syncing, transaction-fees, utxopocket-overview]
glossary_refs: [confirmation, mempool]
keywords: [testnet, faucet, testing]
---

## Purpose of faucets
Faucets provide disposable test coins for development and QA. They are useful for validating flows, not for modeling real market conditions perfectly.

## Good testing habits
- Use separate wallets for each scenario.
- Record txids and expected confirmation windows.
- Expect intermittent faucet reliability and policy limits.

## UtxoPocket context
When testing sync behavior, keep transport policy aligned with the selected mode. In Tor mode, sync should fail closed if Tor is unavailable; there is no automatic fallback to another transport.

## Action checklist
- [ ] Keep faucet and production workflows strictly separated.
- [ ] Label test funds clearly to avoid confusion.
- [ ] Reproduce critical cases on regtest for determinism.
