---
id: taproot-privacy-model
title: Taproot privacy model
summary: Understand key‑path vs script‑path spends and pick defaults that minimize what you reveal on‑chain.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [script-type-tradeoffs, address-format-fingerprints]
glossary_refs: [taproot, key-path, script-path]
keywords: [taproot, privacy, script]
---

## Why it matters
Taproot improves privacy when you spend via the key path: observers only see a public key spend. Revealing a script path discloses part of your policy. Choose defaults that favor key‑path while keeping recovery options.

## Core guidance
- Prefer key‑path spends for routine payments to keep scripts hidden.
- Use script‑path for time‑locks, recovery, or complex controls; expect those conditions to be public.
- Keep script trees minimal and standard to avoid relay policy issues.
- Don’t mix unrelated script families within the same compartment.

## Practical tips
- Label outputs with intended spend path to avoid accidental reveals later.
- Align signer and wallet policy so key‑path remains usable; test small PSBTs.
- Monitor script‑path usage in history and quarantine change if disclosure matters.

## Action checklist
- [ ] Default to key‑path where policy allows.
- [ ] Document when script‑path is intended and why.
- [ ] Test round‑trips with your signer for both paths.

