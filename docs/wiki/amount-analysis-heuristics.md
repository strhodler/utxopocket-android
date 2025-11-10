---
id: amount-analysis-heuristics
title: Amount analysis heuristics
summary: What amounts reveal on‑chain and how to reduce fingerprinting when drafting transactions.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, change-output-hygiene]
glossary_refs: [dust]
keywords: [amounts, fingerprints, privacy]
---

## Why it matters
Output amounts can hint at who you paid and when (exact price points, round numbers, recurring patterns). Combined with change detection, observers infer ownership and behavior.

## What leaks
- Exact price matches that align with public catalogs or invoices.
- Highly rounded amounts that stand out within a cluster.
- Tiny change outputs (near dust) that identify which output is change.

## Mitigations when drafting
- Prefer exact spends from appropriately sized UTXOs to avoid small change.
- If change is unavoidable, keep it clearly above dust and return it to the same compartment.
- Avoid repeatedly emitting the same distinctive amounts from one compartment.
- Plan consolidations separately so they don’t distort payment sizes.

## Review checklist
- [ ] Does the draft avoid near‑dust change?
- [ ] Are inputs from a single compartment?
- [ ] Does the amount pattern match prior history undesirably?
- [ ] Are labels present to preserve intent without over‑sharing?

