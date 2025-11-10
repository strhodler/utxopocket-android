---
id: gap-limit
title: Gap limit
summary: The maximum number of consecutive unused addresses a wallet will scan when discovering funds.
related: [watch-only-restoration]
keywords: [discovery, scanning]
---

The gap limit determines how far ahead a wallet looks for activity on unused addresses. If set too low, historical funds may be missed during watch‑only restores; if set too high, scans get slower. Tune it to your usage and verify with test restores.

