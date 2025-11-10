---
id: transaction-standardness-vs-consensus
title: Transaction standardness vs consensus
summary: Understand relay policy vs. consensus rules so your transactions relay and confirm reliably without leaking privacy.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, mempool-variance-and-policy, policy-risk-register]
glossary_refs: [standardness, mempool]
keywords: [policy, consensus, relay, standardness]
---

## Why it matters
Consensus rules define what is valid. Policy (standardness) defines what most nodes will relay and mine. A transaction can be valid but fail to relay if it violates common policy. Knowing the difference prevents stalls, needless replacements, and privacy‑harming retries.

## Consensus vs. policy
- Consensus: Global rules all nodes enforce (e.g., signature checks, block weight limits). If broken, the transaction is invalid everywhere.
- Policy (standardness): Local relay/mining preferences (e.g., dust limits, script templates, minimum feerate). If broken, many peers refuse to relay; some miners might still include it.

## Common policy pitfalls
- Dust outputs: Creating outputs below policy dust threshold; peers will not relay them.
- Too‑low feerate: Below minimum relay feerate; transaction lingers locally or is dropped.
- Non‑standard scripts: Exotic or non‑templated scripts (bare multisig, unusual tapscripts) may be rejected by relays.
- Oversized or unbalanced packages: Large ancestors/descendants or odd replacement patterns run into package/ancestor limits.
- Malformed change: Tiny change due to poor selection rounds back into dust or triggers extra replacements.

## Safe construction patterns
- Use standard script types (P2WPKH, P2TR) unless you fully understand the policy impact.
- Keep outputs above dust; if not economical, add value to avoid dust or avoid creating the output.
- Target a feerate consistent with mempool conditions and your urgency; see the fee selection guide.
- Enable RBF for time‑sensitive sends to adjust feerate without new addresses or merges.
- Validate final weight/size and package relationships before broadcast.

## Troubleshooting & mitigation
- Stuck due to low feerate: Use RBF to bump; if RBF was not enabled, consider CPFP from change if it preserves compartment boundaries.
- Rejected for policy: Rebuild with standard scripts, adequate feerate, and no dust. Avoid merging extra inputs “just in case” if it breaks privacy rules.
- Mixed script wallets: Avoid mixing legacy and modern script types within a single spend; it creates fingerprints and may hit differing policy edges.

## Action checklist
- [ ] Avoid dust; keep all outputs above policy thresholds.
- [ ] Use standard scripts and enable RBF for urgent sends.
- [ ] Select a feerate that relays and confirms within your window.
- [ ] Verify package/ancestor limits are respected.
