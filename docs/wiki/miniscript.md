---
id: miniscript
title: Miniscript basics
summary: What Miniscript is, why policy descriptors matter, and how watch-only tools can analyze spending conditions safely.
category_id: bitcoin-foundations
category_title: Bitcoin foundations
category_description: Foundational concepts that explain how Bitcoin works and evolves.
related: [spending-policies, descriptors-advanced, multisig-portability, bitcoin-future-tech]
glossary_refs: [miniscript, policy-descriptor, script]
keywords: [miniscript, policy, descriptors]
---

## Policy language, not a new chain rule
Miniscript is a structured way to express Bitcoin Script policies so software can reason about safety, satisfactions, and edge cases.

## Why it helps operations
Explicit policy descriptors improve review, portability, and recovery because spending conditions are easier to inspect and compare.

## Watch-only relevance
Even without keys, watch-only systems can validate policy shape, detect risky templates, and prepare clearer PSBT reviews.

## Action checklist
- [ ] Prefer explicit policy descriptors for complex scripts.
- [ ] Review policy assumptions before funding addresses.
- [ ] Keep portability notes for multisig teams.
