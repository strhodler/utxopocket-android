---
id: policy-risk-register
title: Policy risk register
summary: Common wallet policy pitfalls and how to avoid relay or privacy failures in practice.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [transaction-standardness-vs-consensus, fee-selection-playbook, mempool-variance-and-policy]
glossary_refs: [standardness, dust, vbytes]
keywords: [policy, standardness, fees]
---

## Why it matters
Transactions can be valid by consensus yet fail to relay due to policy. Others leak privacy due to dust or odd script patterns. Cataloging risks helps avoid surprises.

## Common pitfalls
- Non‑standard scripts or uncommon sizes that some peers reject.
- Dust outputs that are uneconomical to spend and create fingerprints.
- RBF settings that don’t allow later fee bumps when timelines change.
- Large input sets that trigger limits or strengthen ownership heuristics.

## Mitigations
- Stay within standardness; favor common script templates.
- Right‑size fees using vbytes estimates; leave room for RBF where appropriate.
- Avoid dust; consolidate in low‑fee windows within one compartment.
- Prefer smaller input counts without merging compartments.

## Action checklist
- [ ] Conform to standardness rules.
- [ ] Estimate vbytes and set flexible fees.
- [ ] Avoid dust creation; plan consolidation.
- [ ] Keep inputs minimal and compartment‑safe.
