---
id: bbqr-label-export
title: BBQR for label exports
summary: Use compressed BBQR fragments to move BIP-329 label backups faster across wallets.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [label-export-bip329-workflows, psbt-airgap-basics]
glossary_refs: [bbqr, bip-329, ur]
keywords: [labels, qr, sparrow, bip-329]
---

## Why BBQR
- Compresses JSON payloads so multi-frame QR exports take fewer scans than UR.
- Matches Sparrow's BBQR format, keeping BIP-329 label payloads interoperable.

## How UtxoPocket uses it
- The label export screen now lets you choose BBQR or UR; BBQR is the default and uses ZLIB with a base32 fallback plus the same fragment length Sparrow ships by default.
- The payload is the same JSONL produced by file export (`type`, `ref`, `label`, `origin`, `spendable`).
- The scanner already understands BBQR fragments alongside UR; if the receiving wallet cannot read BBQR, toggle to UR or export a file instead.

## Usage checklist
- Pick BBQR unless the receiver only supports UR.
- Keep the animated sequence visible until the frame counter completes; large payloads may still need several dozen frames.
- Prefer the file export path for very large label sets to avoid long scans.

## Notes and limits
- BBQR headers include part counts and type info; editing fragments will break decoding.
- Compression happens on-device only; no telemetry is emitted during export or import.
- Treat label exports as sensitive backups; labels can reveal provenance even without keys.
