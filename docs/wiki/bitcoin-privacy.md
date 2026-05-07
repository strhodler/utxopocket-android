---
id: bitcoin-privacy
title: Bitcoin privacy fundamentals
summary: A practical framework for reducing on-chain linkability through behavior, labeling, and transport hygiene.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [utxo-segregation-playbook, address-reuse-casebook, labeling-metadata, operational-hygiene]
glossary_refs: [anonymity-set, address-reuse, coin-control, tor]
keywords: [privacy, metadata, utxo]
---

## Privacy is process discipline
Bitcoin is transparent by design. Privacy comes from reducing unnecessary links between your activities, not from any single app setting.

## Common leak paths
- Address reuse and repeated spend patterns.
- Compartment merges caused by careless input selection.
- Network metadata leaks from direct or inconsistent routing.

## Watch-only advantage
Watch-only tools help you inspect history, labels, and planned spends before signing. This extra review layer prevents many avoidable leaks.

## Action checklist
- [ ] Separate UTXOs by purpose and risk.
- [ ] Label intent at receive time, not later.
- [ ] Keep Tor transport and manual review as defaults.
