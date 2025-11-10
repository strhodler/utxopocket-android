---
id: multipath
title: Multipath descriptor (BIP‑389)
summary: Descriptor syntax that encodes external and change branches in a single definition using tuples like `/<0;1>/*`.
related: [watch-only-restoration, descriptors-advanced]
keywords: [bip389, multipath, descriptor]
---

Multipath descriptors simplify imports by bundling both receiving and change branches. Tools must agree on branch order and scanning to avoid missed funds; see the restoration guide.

