---
id: mempool-variance-and-policy
title: Mempool variance and policy
summary: Why transactions relay on some peers but not others, and how to plan around policy differences.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [transaction-standardness-vs-consensus, rbf-cpfp-strategies, fee-selection-playbook]
glossary_refs: [mempool, standardness, rbf, cpfp, feerate]
keywords: [mempool, policy, relay]
---

## Why it matters
Nodes don’t all accept or keep the same transactions. Policy knobs (limits, replace rules, dust thresholds) vary, so a broadcast may appear to “vanish” on some peers. Planning for variance improves reliability and privacy.

## Core guidance
- Keep transactions standard; non‑standard spends may fail to relay even if they are valid by consensus.
- Choose feerates appropriate for size and urgency; leave room for RBF when timelines are uncertain.
- Use CPFP strategically if a low‑fee parent needs help; avoid merging compartments in the child.
- Monitor confirmation across multiple sources you control; don’t leak queries to random public servers.

## Practical checks
- Estimate size in vbytes and compare against feerate targets before drafting.
- Verify RBF signaling when you might need to bump.
- Avoid dust outputs; prune change that would be uneconomical to spend.

## Action checklist
- [ ] Conform to standardness; avoid dust.
- [ ] Right‑size fees and enable RBF if needed.
- [ ] Use CPFP only within the same compartment.
- [ ] Track status via your own backend(s).

