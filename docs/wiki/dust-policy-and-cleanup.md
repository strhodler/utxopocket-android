---
id: dust-policy-and-cleanup
title: Dust policy and cleanup
summary: Prevent creating dust and clean it safely when necessary without linking compartments.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, consolidation-strategy, change-output-hygiene]
glossary_refs: [dust, consolidation, feerate]
keywords: [dust, fees, consolidation]
---

## Why it matters
Dust increases future fees and creates recognizable patterns. Avoid creating it, and when it exists, clean it during favorable conditions without leaking privacy.

## Prevention
- Size outputs above common dust thresholds, accounting for script type.
- Favor exact/near‑exact spends to avoid tiny change.
- Pre‑plan consolidation windows instead of pulling tiny inputs into urgent payments.

## Cleanup strategy
- Wait for low‑fee periods; target a feerate appropriate for batch consolidation.
- Consolidate within the same compartment and script family.
- Label consolidation intent and quarantine resulting change until it is economical to spend.

## Action checklist
- [ ] Avoid producing dust in new drafts.
- [ ] Consolidate dust only within one compartment.
- [ ] Use low‑fee windows; label intent clearly.

