---
id: key-fingerprint
title: Key fingerprint (BIP32)
summary: A 4‑byte identifier of a master key used to tag origins in descriptors and PSBTs.
related: [descriptor-maps-and-recovery, descriptors-advanced]
keywords: [fingerprint, bip32, origin]
---

The fingerprint helps bind xpubs to their source master key when sharing descriptors or PSBTs. Together with a derivation path, it forms the “key origin” that lets tools verify they reference the intended account.

