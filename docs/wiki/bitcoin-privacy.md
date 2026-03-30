---
id: bitcoin-privacy
title: Bitcoin privacy fundamentals
summary: A practical framework for reducing on-chain linkability through behavior, labeling, transport hygiene, and local privacy review.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [wallet-analysis, utxo-segregation-playbook, address-reuse-casebook, labeling-metadata, operational-hygiene]
glossary_refs: [anonymity-set, address-reuse, coin-control, tor]
keywords: [privacy, metadata, utxo, heuristics]
---

## Privacy is process discipline
Bitcoin is transparent by design. Privacy comes from reducing unnecessary links between your activities, not from any single app setting.

## Common leak paths
- Address reuse and repeated spend patterns.
- Compartment merges caused by careless input selection.
- Network metadata leaks from direct or inconsistent routing.

## Watch-only advantage
Watch-only tools help you inspect history, labels, and planned spends before signing. This extra review layer prevents many avoidable leaks.

## How to use in-app privacy findings
UtxoPocket surfaces wallet, transaction, and UTXO privacy findings as **review aids**. They help prioritize manual checks, but they are not chain truth.

- Deterministic findings describe direct facts from the local snapshot (for example, reused addresses or dust-sized outputs).
- Heuristic findings (for example, probable change or multi-input ownership exposure) are probabilistic and can be wrong.
- CoinJoin-like, PayJoin-like, and multisig flows can break simple heuristics; always verify context before acting.
- Findings are local-first in v1: no ancestry tracing, entity lookups, or extra network calls are used.
- There is no public numeric privacy score; severity and confidence are meant to guide review order.

## Action checklist
- [ ] Separate UTXOs by purpose and risk.
- [ ] Label intent at receive time, not later.
- [ ] Keep Tor transport and manual review as defaults.
- [ ] Treat heuristic findings as prompts to investigate, not as final attribution.
