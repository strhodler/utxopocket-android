---
id: full-rescan
title: Full rescan
summary: A wallet-wide sync that replays all known addresses up to a chosen gap limit.
related: [gap-limit, watch-only]
keywords: [discovery, sync, address-reuse, backlog]
---

A full rescan re-derives receive and change addresses up to the configured gap limit and asks the backend to replay their history. Use it when balances look stale, when other wallets may have revealed addresses, or after changing nodes. Larger gap limits increase coverage but take longer and reveal more addresses to the server, so pick the smallest window that fits your usage. !*** End Patch】്
