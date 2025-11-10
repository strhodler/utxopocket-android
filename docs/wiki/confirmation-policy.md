---
id: confirmation-policy
title: Confirmation policy
summary: Practical confirmation thresholds by risk, reorg awareness, and how to reason about security for different use‑cases.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [fee-selection-playbook, transaction-standardness-vs-consensus, mempool-variance-and-policy]
glossary_refs: [confirmation, mempool]
keywords: [confirmations, reorg, risk]
---

## Why it matters
Security increases with each block that builds on your transaction. Picking confirmation thresholds by risk protects you from double‑spends and reorgs without delaying routine operations unnecessarily.

## Risk tiers and examples
- Low risk (informational): 0–1 conf for UI hints; never assume finality. Do not trigger irreversible actions.
- Routine payments: 1–3 conf depending on value and counterparty risk.
- High value or long‑tail risk: 6+ conf; more if your policy or jurisdiction requires it.

## Reorg awareness
- Short reorgs happen; avoid treating 1 conf as guaranteed finality for critical flows.
- Consider additional buffers during volatile periods or after network events.
- For chained transactions (RBF/CPFP), ensure parents are reasonably buried before taking external commitments.

## Operational guidance
- Align UI badges and alerts with your chosen thresholds.
- For watch‑only, display both mempool acceptance and confirmation depth; make the difference explicit.
- Record confirmation height/time in labels to support audits.

## Action checklist
- [ ] Define confirmation thresholds per use‑case (low, routine, high value).
- [ ] Reflect thresholds consistently in UI and automation.
- [ ] Treat 0‑conf as untrusted; avoid irreversible actions until buried sufficiently.
