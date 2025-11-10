---
id: non-kyc-acquisition
title: Non-KYC acquisition
summary: Practical steps to source bitcoin without handing over your identity and without mixing clean coins with tainted ones.
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on-chain exposure and keep compartments isolated.
related: [bitcoin-privacy, utxo-basics, address-reuse-casebook]
glossary_refs: [non-kyc, coin-control]
keywords: [non-kyc, p2p, coin control, cash]
---

## Why it matters
Buying through KYC funnels permanently links your legal identity to specific UTXOs and leaks payment fingerprints that tend to be resold. Non-KYC flows are about regaining basic financial privacy, not about evading the law. If you compartmentalize early, later transactions become harder to cluster.

## Non-KYC avenues
- **Cash-for-coin meetups** – face-to-face, neutral locations, verify banknotes, no smartphones pointing at you.
- **Reputation-based P2P desks** – escrow platforms that allow pseudonymous accounts; always connect via Tor/VPN, use burner email.
- **Trusted remittances** – counterparties with fresh payouts (mined, OTC, or peer pools) willing to sell small batches directly.
- **Earn sats** – request payment for goods/services directly to your descriptor; label income UTXOs clearly.

## Good practice
1. **Separate descriptors** – dedicate one watch-only descriptor per provenance bucket (non-KYC, swr-KYC, donations, etc.).
2. **Document provenance** – BIP-329 labels with date, counterpart, price, and conditions; they inform future spending policy.
3. **Stay patient** – insist on on-chain confirmations; never reuse receive addresses to “speed things up”.
4. **Guard your change** – when spending, steer change outputs back into the same compartment to avoid contaminating cold buckets.
5. **Audit periodically** – reconcile receipts vs. UTXO set so you can prove ownership without revealing counterparties.

## Action checklist
- [ ] Create (or import) a dedicated descriptor for non-KYC funds; never mix them with exchange withdrawals.
- [ ] Keep an encrypted notebook with transaction details, proof of settlement, and agreed dispute rules.
- [ ] Prearrange meetup logistics (pricing bands, acceptable chain conditions, confirmation count).
- [ ] Schedule a quarterly review to consolidate small UTXOs or quarantine suspicious ones before they become dust.
