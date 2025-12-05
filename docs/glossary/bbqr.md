---
id: bbqr
title: BBQR
summary: A compressed, typed multi-part QR format derived from Sparrow's Binary Block QR.
related: [bip-329, ur]
keywords: [qr, airgap, labels]
---

BBQR (Binary Block QR) wraps data in QR fragments that include encoding, type, and sequence metadata so larger payloads can scan reliably. UtxoPocket uses BBQR to export BIP-329 label backups in fewer frames while keeping UR available for wallets that do not support BBQR.
