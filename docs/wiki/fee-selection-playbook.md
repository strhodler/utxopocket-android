---
id: fee-selection-playbook
title: Fee selection playbook
summary: Choose feerates by urgency, size, and policy while avoiding privacy leaks and costly replacements.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [rbf-cpfp-strategies, utxo-segregation-playbook, mempool-variance-and-policy]
glossary_refs: [feerate, mempool, rbf, cpfp]
keywords: [fees, feerate, mempool, rbf, cpfp]
---

## Why it matters
Paying too much wastes funds; paying too little delays confirmation or forces replacements. Smart fee selection balances urgency, transaction size, and network conditions, without violating your privacy rules or leaking compartments.

## Read the mempool
- Prefer your own node’s mempool view (over Tor) to avoid leaking queries.
- Look at current feerate buckets (sats/vB) and the depth needed for your target confirmation window.
- Remember propagation and policy filters: very low feerates may not relay reliably.

## Pick a target feerate
1. Define urgency: next block, same day, or flexible window.
2. Estimate virtual size: more inputs increase size disproportionately; coin control can shrink or grow fees.
3. Select a feerate band that clears within your window, with a buffer for volatility.

## Privacy‑aware tradeoffs
- Avoid merging compartments just to lower fees; privacy costs more than a few sats.
- If inputs are many and small, consider a prior consolidation during a mempool lull instead of paying peak rates now.
- Verify change policy: return change to the same bucket to prevent toxic links.

## Replacement policy considerations
- If urgency is high, enable RBF at construction so you can bump later without new addresses.
- If stuck and RBF wasn’t enabled, consider CPFP from the change output (same bucket only) when safe.
- Document replacements in labels (reason, prior feerate) for future audits.

## Action checklist
- [ ] Check your own node’s mempool view and pick a feerate window.
- [ ] Use coin control to minimize inputs without crossing compartments.
- [ ] Set RBF if time‑sensitive; otherwise leave room for CPFP via change.
- [ ] Verify change returns to the correct descriptor and is labeled.
