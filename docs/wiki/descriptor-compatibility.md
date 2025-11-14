---
id: descriptor-compatibility
title: Descriptor compatibility with UtxoPocket
summary: Supported descriptor templates, how combined exports are parsed, and how to format descriptors so UtxoPocket can monitor them safely.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [descriptors-advanced, descriptor-maps-and-recovery, bip389-multipath-practical]
glossary_refs: [descriptor, descriptor-checksum, bip-389]
keywords: [descriptor, bip389, watch-only]
---

## Supported templates
UtxoPocket relies on BDK 2.2 to parse descriptor strings. Any descriptor that BDK can interpret without private key material is accepted, including:

- **Single-key templates**: `wpkh()`, `tr()`, `wsh()`, `sh(wpkh())`, `combo()`, `pkh()`.
- **Multisig templates**: `sortedmulti()`/`multi()` descriptors that resolve to standard P2WSH, P2SH, or Taproot scripts.
- **Policy/miniscript descriptors**: As long as the resulting spend path complies with standard relay policy.

Each descriptor must include a checksum (`#xxxxxxx`) and, for derivable wallets, either a wildcard `*` or a BIP-389 multipath segment such as `/<0;1>/*`. View-only imports reject private key material—export xpubs or public miniscript policies instead.

## Combined external + change exports
Some wallets (like Blockstream) present two descriptors at once—one for the external branch and another for change. When you paste such an export into UtxoPocket:

1. Paste both descriptors, one per line (or separated by whitespace).
2. UtxoPocket detects the pair automatically, moves the second descriptor into the “Change descriptor” input, and leaves the external descriptor in the main field.
3. Validation ensures both descriptors share the same key material and derivation structure before continuing.

If you only have a single descriptor, make sure it is either multipath (`/<0;1>/*`) or accompanied by a matching change descriptor so rescan coverage stays complete.

## Multipath descriptors (BIP‑389)
Multipath descriptors collapse multiple branches into a single expression, e.g.:

```
wpkh([f23ab65d/84'/0'/0']xpub.../<0;1>/*)#checksum
```

UtxoPocket recognizes these patterns and treats them as both external and change branches. Do **not** provide a separate change descriptor when using this format; the validator will prompt you to remove it.

## Formatting checklist
- Include the checksum at the end of every descriptor.
- Keep each descriptor on its own line; avoid wrapping mid-line.
- Trim whitespace before and after the descriptor string.
- Ensure that account-level derivations (e.g., `84'/0'/0'`) match the network you plan to monitor.

If validation fails, revisit the source wallet’s descriptor export page and copy the raw text again. You can always reference this article via the “Descriptor compatibility” link inside the Add Wallet flow.
