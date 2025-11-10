---
id: cioh
title: Common‑Input Ownership Heuristic (CIOH)
summary: An inference that all inputs in a transaction belong to one entity; often but not always true.
related: [utxo-segregation-playbook]
keywords: [heuristics, privacy]
---

CIOH underpins many clustering techniques. It becomes unreliable when transactions use privacy methods (e.g., PayJoin or collaborative spends). Avoid merging unrelated inputs to keep CIOH from linking compartments.

