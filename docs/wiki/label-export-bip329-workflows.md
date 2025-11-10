---
id: label-export-bip329-workflows
title: Label export workflows (BIP‑329)
summary: Export, merge, and round‑trip wallet labels safely across apps without leaking metadata.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [provenance-labeling, utxo-segregation-playbook]
glossary_refs: [bip-329]
keywords: [labels, bip-329, provenance]
---

## Why it matters
Labels preserve intent and provenance across tools. Clean exports prevent losing context or leaking sensitive notes when sharing files.

## Core guidance
- Keep English, concise labels; avoid PII. Use consistent schemas (source, intent, compartment).
- Export BIP‑329 JSON from a trusted device; verify fields and counts before sharing.
- When merging, prefer additive updates; keep a backup of prior label sets.
- Re‑import and verify that key UTXOs/transactions show expected labels.

## Practical steps
- Export from app A; inspect JSON for expected keys and redactions.
- Merge with app B’s labels offline; resolve conflicts using timestamps or a source‑of‑truth rule.
- Import into both apps; re‑scan UI for expected tags and search results.

## Action checklist
- [ ] Labels use a consistent schema; no PII.
- [ ] JSON passes a quick sanity check (counts, keys).
- [ ] Merge rules applied; backups kept.
- [ ] Post‑import verification performed.

