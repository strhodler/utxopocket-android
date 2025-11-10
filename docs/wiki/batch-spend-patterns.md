---
id: batch-spend-patterns
title: Batch spend patterns
summary: Balance fee savings from batching against privacy costs; know when batching amplifies clustering.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [address-reuse-casebook, utxo-segregation-playbook]
glossary_refs: [cioh, utxo]
keywords: [batching, fees, privacy]
---

## Why it matters
Batching multiple payments into one transaction can reduce fees but often strengthens ownership heuristics by merging many inputs and revealing recipient sets.

## When batching hurts privacy
- Inputs from different compartments appear together (CIOH leak).
- Repeated recipient sets reveal business relationships.
- Tiny change outputs create dust fingerprints.

## Safer alternatives
- Separate compartments and send in distinct transactions.
- Use exact spends when possible to avoid change.
- If batching is required, source inputs from a single compartment and keep recipient sets generic.

## Action checklist
- [ ] Avoid merging compartments for batching savings alone.
- [ ] Limit input count and avoid dust change.
- [ ] Label intent and recipients to audit future co‑spends.

