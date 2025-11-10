---
id: change-output
title: Change output
summary: The output that returns leftover funds to the spender in a transaction.
related: [change-output-hygiene, utxo-segregation-playbook]
keywords: [change, privacy]
---

A change output returns the difference between selected inputs and payment amount back to the spender. Change must return to the same policy/descriptor that funded the spend; otherwise it links unrelated histories and weakens privacy.

