---
id: utxo
title: UTXO (Unspent Transaction Output)
summary: A discrete coin controlled by a spend condition; the basic unit a wallet selects as input.
related: [utxo-segregation-playbook]
keywords: [coin, input]
---

A UTXO is an individual output from a confirmed transaction that has not yet been spent. Wallets build new transactions by selecting one or more UTXOs as inputs. Good hygiene treats each UTXO as a separate history to avoid leaking links when spending.

