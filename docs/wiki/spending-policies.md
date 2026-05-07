---
id: spending-policies
title: Spending policies and controls
summary: Define explicit policy rules for approvals, confirmations, and UTXO boundaries before funds move.
category_id: safety-operations
category_title: Safety and operations
category_description: Operational practices for resilient, private wallet use.
related: [confirmation-policy, coin-control, miniscript, policy-risk-register]
glossary_refs: [policy-descriptor, confirmation, coin-control]
keywords: [policy, governance, spending]
---

## Policy before transaction
Good policy is pre-committed behavior: who can approve, what risk is acceptable, and which exceptions require escalation.

## Common policy layers
- Value-based approval thresholds.
- Confirmation depth by counterparty risk.
- Compartment boundaries for input selection.
- Emergency freeze conditions.

## Documentation matters
Write policies in plain language and keep them versioned. Teams fail when policy exists only as unwritten assumptions.

## Action checklist
- [ ] Publish approval and confirmation matrices.
- [ ] Encode selection boundaries in wallet procedures.
- [ ] Review policy after major environment changes.
