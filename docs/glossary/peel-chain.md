---
id: peel-chain
title: Peel chain
summary: A pattern where repeated spending peels small amounts from one UTXO, leaving a linkable trail.
related: [change-output-hygiene]
keywords: [pattern, privacy]
---

Peel chains arise when change is repeatedly reused for new payments, creating a long chain of linked transactions. Mitigate by returning change to the same descriptor, consolidating during low‑fee periods, or eliminating change with exact‑match inputs.

