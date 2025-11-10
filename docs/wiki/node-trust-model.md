---
id: node-trust-model
title: Node trust model
summary: Run your own validating node behind Tor to minimize third‑party trust; understand DIY vs plug‑and‑play tradeoffs, descriptor migration, and routine health checks for watch‑only.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [electrum-servers, watch-only-restoration, pruned-vs-full-nodes]
glossary_refs: [full-node, pruned-node, descriptor]
keywords: [node, validation, electrum, tor, descriptor]
---

## Why it matters
Wallet privacy and reliability depend on who answers your chain and mempool questions. Running your own validating node (preferably reachable only via Tor) removes third‑party observers and aligns your wallet view with consensus rules. Watch‑only monitoring also relies on accurate descriptor scans that a trustworthy backend must honor.

## Trust spectrum
- Public servers: Fast to try, but they see your queries and may apply unknown policy filters. Not recommended beyond experimentation.
- Hosted node from a vendor: Better than public, but still a third party with visibility into your usage and failure modes outside your control.
- Self‑hosted Electrum server on top of your node: Strong privacy when accessed over Tor; requires upkeep to keep indexes consistent.
- Full node at home (or pruned): Best trust model. You validate blocks/transactions and set the policy boundaries you accept.

## DIY vs plug‑and‑play
- DIY full/pruned node: Maximum control, predictable updates, and clear logs. Requires maintenance and backups of data directories.
- Appliance/plug‑and‑play: Lower setup cost, but opaque update cycles and limited visibility into indexing issues. Verify Tor routing, versions, and exposed services before trusting with wallet data.

## Watch‑only implications
- Descriptors: Keep descriptor maps and checksums so restores reproduce the same address space across backends.
- Discovery and gap limits: Configure sensible gap limits; pruned nodes may constrain deep rescans. Plan periodic targeted rescans when adding historic descriptors.
- Consistency: Ensure your wallet, node, and Electrum index agree on network, chain tip, and script policies. Mismatches create false negatives in balances and history.

## Routine health checks
- Chain sync: Headers and blocks fully synchronized; reorg handling tested.
- Mempool view: Feerate buckets look plausible vs recent blocks; policy not excessively restrictive.
- Tor connectivity: All wallet RPC and Electrum traffic routes via Tor; onion endpoints respond.
- Electrum index: Script/address queries return quickly and match node data; no stale indexes.
- Descriptor coverage: Spot‑check derived addresses across ranges; verify gap limit behavior and rescan completeness.

## Migration checklist (backend changes)
1. Snapshot descriptor maps and checksums.
2. Verify the new backend’s network, chain height, and policy settings.
3. Restore watch‑only descriptors; run a bounded rescan sized to your usage and gap limit.
4. Compare balances and recent history across backends; investigate discrepancies before switching.
5. Keep Tor‑only access; do not expose services on clearnet.

## Action checklist
- [ ] Use your own full (or pruned) node reachable only via Tor.
- [ ] Run an Electrum server you control and point UtxoPocket to it.
- [ ] Maintain descriptor maps + checksums for reproducible restores.
- [ ] Periodically verify discovery ranges and rescan when adding old descriptors.
- [ ] Monitor node, mempool, and index health before important operations.
