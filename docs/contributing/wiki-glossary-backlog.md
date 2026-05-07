# Wiki/Glossary Backlog — UtxoPocket Authoring Plan

Date: 2025‑11‑07

## Purpose
- Keep the public wiki/glossary aligned with best layer‑1 privacy practices from reputable sources.
- Ensure every addition is rewritten in English with UtxoPocket’s voice (watch-only, descriptor-driven, Tor-only) and published as Markdown under `/docs`.

## Current Coverage Snapshot
- UTXO model, wallet types, keys & seeds, descriptor basics.
- Tor usage, Electrum presets, PSBT primer.
- Fee heuristics (RBF/CPFP basics) and labeling metadata.

## Opportunity Areas (L1 only)
1. **Acquisition & sourcing** – Non-KYC flows, provenance tracking, stacks separation.  
2. **Self-custody hygiene** – Seed lifecycle, passphrases, encrypted backups, Tor-enabled wallet discipline.  
3. **UTXO segregation** – Coin control, BIP-329 labeling conventions, address reuse avoidance.  
4. **Node trust model** – DIY vs plug-and-play, descriptor migration, backend checks.  
5. **On-chain heuristics** – Toxic change, script fingerprints, poisoned outputs.  
6. **Expanded glossary** – Standardness vs consensus, gap limit, dust, CIOH, etc.

## Out of Scope
- Lightning, sidechains, federated layers, wallet/product reviews, custodial flows.
- Anything that encourages telemetry, KYC optimization, or centralized custody.

---

## Wiki Interlinking Strategy
1. **Glossary links** – each topic references glossary slugs via frontmatter `glossary_refs`; renderer auto-links first occurrence per section.
2. **Related topics** – frontmatter `related` holds 2–6 topic IDs; UI shows the cards at the end of each article.
3. **Cross-ui entry points** – settings help icons → relevant wiki sections; timeline badges → related wiki topics.
4. **Linting** – future check should fail on broken IDs, missing glossary refs, or unknown related topics.

## Leveraging External Interlinks
- When reviewing reputable educational material, note topic interlinks and propose mutual `related` references or glossary additions when they fit our L1 scope.
- Always review manually to ensure the relationship fits a watch-only, L1 context before landing changes.

---

## Markdown Migration
- All new content lives in `/docs/wiki/<id>.md` (articles) or `/docs/glossary/<term>.md` (definitions).
- Frontmatter template:
  ```yaml
  ---
  id: descriptor-integrity
  title: Descriptor integrity checks
  summary: "Short summary"
  category_id: privacy-toolkit
  category_title: Privacy toolkit
  category_description: "Optional longer blurb"
  related: [watch-only-restoration, descriptors-advanced]
  glossary_refs: [gap-limit, cioh]
  keywords: [privacy, descriptor]
  ---
  ```
- Body uses Markdown headings, bullet lists, and checklists. Avoid raw HTML; renderer converts basic Markdown to readable text (bullets/checklists already supported).
- Migration plan:
  1. New topics -> Markdown only.
  2. Runtime is markdown-only for wiki and glossary; no Kotlin fallback content remains in repositories.
  3. Legacy article migration backlog is tracked in `plans/LATER-2026-03-13-p4-legacy-wiki-topic-migration-with-docs-steward.md`.
  4. Keep tests and reference checks green to catch malformed frontmatter or broken `related`/`glossary_refs` links.

---

## Editorial Workflow
- Contributors or maintainers add ideas to this backlog via PRs.
- Docs maintainers pick the highest-priority items, create or update Markdown under `/docs`, verify rendering, and mark the backlog entry as shipped (linking to the file/path).
- Repeat as needed; this backlog acts as the single source of truth for future documentation work.

---

## Backlog — Wiki/Glossary Tasks

| Status | ID / Type | Summary & Notes | Priority | Suggested Links / Glossary |
|--------|-----------|-----------------|----------|----------------------------|
| ✅ | wiki `non-kyc-acquisition` | How to source bitcoin privately, separate stacks, and log provenance. Implemented in `/docs/wiki/non-kyc-acquisition.md`. | High | Related: `bitcoin-privacy`, `utxo-basics`; Glossary: `non-kyc`, `coin-control`. |
| ✅ | wiki `self-custody-hygiene` | Watch-only self‑custody habits: seed lifecycle, passphrases, Tor‑enabled wallets, encrypted backups, label retention, checklist. Implemented in `/docs/wiki/self-custody-hygiene.md`. | High | Related: `keys-and-seeds`, `bitcoin-privacy`; Glossary: `seed-phrase`, `passphrase`, `tor`. |
| ✅ | wiki `utxo-segregation-playbook` | Segregation rules for compartments, BIP‑329 labeling, coin control, address reuse warnings, and change hygiene. Implemented in `/docs/wiki/utxo-segregation-playbook.md`. | High | Related: `utxo-basics`, `labeling-metadata`; Glossary: `utxo`, `change-output`, `address-reuse`. |
| ✅ | wiki `node-trust-model` | Summarize “Scrutinising your Bitcoin”: own-node trust model, DIY vs plug-and-play, descriptor migration, routine backend checks, risk table. Implemented in `/docs/wiki/node-trust-model.md`. | Medium | Related: `electrum-servers`, `watch-only-restoration`; Glossary: `full-node`, `pruned-node`. |
| ✅ | glossary `address-reuse` | Definition covering deanonymization risk + pointer to the segregation article. Implemented in `/docs/glossary/address-reuse.md`. | Medium | References `utxo-segregation-playbook`. |
| ✅ | glossary `full-node` | Definition framed for UtxoPocket (validating vs trusting third parties, descriptor rescans). Implemented in `/docs/glossary/full-node.md`. | Medium | References `node-trust-model`. |

> When an item is shipped, mark it ✅ and link to the commit/PR. The remaining entries feed the wiki editor queue.

| ✅ | wiki `transaction-standardness-vs-consensus` | Clarify policy vs. consensus rules, why some transactions fail to relay, and safe patterns for wallet construction. Implemented in `/docs/wiki/transaction-standardness-vs-consensus.md`. | Medium | Related: `fee-selection-playbook`; Glossary: `standardness`. |
| ✅ | wiki `rbf-cpfp-strategies` | Practical guidance on Replace‑by‑Fee and Child‑Pays‑for‑Parent: when to use each, etiquette, and risks. Implemented in `/docs/wiki/rbf-cpfp-strategies.md`. | High | Related: `fee-selection-playbook`; Glossary: `rbf`, `cpfp`. |
| ✅ | wiki `change-output-hygiene` | How to recognize and contain toxic change, return paths, and policy alignment across buckets. Implemented in `/docs/wiki/change-output-hygiene.md`. | High | Related: `utxo-segregation-playbook`; Glossary: `change-output`, `toxic-change`. |
| ✅ | wiki `address-format-fingerprints` | What scripts leak (P2PKH vs P2SH vs P2WPKH vs P2TR), pros/cons for privacy and fees. Implemented in `/docs/wiki/address-format-fingerprints.md`. | Medium | Related: `descriptors-advanced`; Glossary: `bech32`, `bech32m`. |
| ✅ | wiki `dust-policy-and-cleanup` | Define policy/economic dust, prevention, safe cleanup windows, and consolidation tips. Implemented in `/docs/wiki/dust-policy-and-cleanup.md`. | Medium | Related: `fee-selection-playbook`; Glossary: `dust`. |
| ✅ | wiki `poisoned-utxos-quarantine` | How to identify and quarantine suspicious coins; when to avoid co‑spends or quarantine in separate descriptors. Implemented in `/docs/wiki/poisoned-utxos-quarantine.md`. | Medium | Related: `utxo-segregation-playbook`. |
| ✅ | wiki `fee-selection-playbook` | Read the mempool, pick target feerates by urgency, size vs. inputs tradeoffs, and replacement policies. Implemented in `/docs/wiki/fee-selection-playbook.md`. | High | Related: `rbf-cpfp-strategies`; Glossary: `feerate`, `mempool`. |
| ✅ | wiki `descriptors-advanced` | Checksums, multipath (BIP‑389), policy descriptors, account maps, and recovery notes for watch‑only monitoring. Implemented in `/docs/wiki/descriptors-advanced.md`. | Medium | Related: `watch-only-restoration`; Glossary: `descriptor-checksum`. |
| ✅ | glossary `standardness` | Policy rules used by nodes/relays distinct from consensus rules; link to the standardness article. Implemented in `/docs/glossary/standardness.md`. | Medium | References `transaction-standardness-vs-consensus`. |
| ✅ | glossary `gap-limit` | Wallet scanning limit for address discovery; implications for watch-only rescans and related verification checks. Implemented in `/docs/glossary/gap-limit.md`. | Medium | References `descriptors-advanced`. |
| ✅ | glossary `toxic-change` | Change that links compartments or policies, and how to avoid it; link to change hygiene. Implemented in `/docs/glossary/toxic-change.md`. | Medium | References `change-output-hygiene`. |
| ✅ | wiki `confirmation-policy` | Security model by confirmations: risk tiers, reorg awareness, and practical thresholds by use‑case. Implemented in `/docs/wiki/confirmation-policy.md`. | Medium | Related: `fee-selection-playbook`; Glossary: `confirmation`. |
| ✅ | wiki `script-type-tradeoffs` | Privacy and efficiency tradeoffs among script types; mixing scripts in one wallet and how it leaks. Implemented in `/docs/wiki/script-type-tradeoffs.md`. | Medium | Related: `address-format-fingerprints`; Glossary: `script`. |
| ✅ | wiki `provenance-labeling` | Label schema examples for BIP‑329 (source, intent, jurisdiction) and migration tips for legacy wallets. Implemented in `/docs/wiki/provenance-labeling.md`. | High | Related: `labeling-metadata`, `utxo-segregation-playbook`; Glossary: `bip-329`. |
| ✅ | wiki `incoming-tx-detection` | Tor-only lightweight Electrum watcher for early incoming states, with placeholder UX reconciled only by canonical BDK sync and receive-screen checks to advance addresses safely. Implemented in `/docs/wiki/incoming-tx-detection.md`. | High | Related: `electrum-servers`, `address-discovery-and-gap-limit`; Glossary: `electrum-server`, `tor`, `gap-limit`. |
| ✅ | wiki `descriptor-maps-and-recovery` | Maintaining descriptor maps, account origins, and checksums to guarantee reproducible watch‑only restores. Implemented in `/docs/wiki/descriptor-maps-and-recovery.md`. | Medium | Related: `descriptors-advanced`, `watch-only-restoration`; Glossary: `descriptor-checksum`. |
| ✅ | wiki `compartment-migration` | Safe migration between descriptors/buckets without leaking links (timing, fees, change policy). Implemented in `/docs/wiki/compartment-migration.md`. | Medium | Related: `utxo-segregation-playbook`; Glossary: `compartment`. |
| ✅ | wiki `pruned-vs-full-nodes` | Storage/bandwidth profiles, verification guarantees, and watch‑only scanning considerations. Implemented in `/docs/wiki/pruned-vs-full-nodes.md`. | Medium | Related: `node-trust-model`; Glossary: `pruned-node`, `full-node`. |
| ✅ | wiki `address-reuse-casebook` | Real‑world privacy failures from address reuse and how to prevent them (education‑focused). Implemented in `/docs/wiki/address-reuse-casebook.md`. | Medium | Related: `change-output-hygiene`; Glossary: `address-reuse`. |
| ✅ | glossary `confirmation` | What a confirmation is, why 0‑conf is unsafe, and pointers to the confirmation policy article. Implemented in `/docs/glossary/confirmation.md`. | Medium | References `confirmation-policy`. |
| ✅ | glossary `descriptor-checksum` | Short note on descriptor checksums and why they matter for reproducible monitoring/restores. Implemented in `/docs/glossary/descriptor-checksum.md`. | Medium | References `descriptors-advanced`. |
| ✅ | glossary `compartment` | A labeled purpose‑bucket for UTXOs; ties into segregation and change policy. Implemented in `/docs/glossary/compartment.md`. | Medium | References `utxo-segregation-playbook`. |
| ✅ | wiki `mempool-variance-and-policy` | How node policy differs across the network, why a tx relays on some peers but not others, and mitigation tips. Implemented in `/docs/wiki/mempool-variance-and-policy.md`. | Medium | Related: `transaction-standardness-vs-consensus`; Glossary: `mempool`. |
| ✅ | wiki `batch-spend-patterns` | Pros/cons of batching for fees vs. privacy; when batching amplifies clustering and safer alternatives. Implemented in `/docs/wiki/batch-spend-patterns.md`. | Medium | Related: `address-reuse-casebook`. |
| ✅ | wiki `utxo-lifecycle-audit` | From acquisition to disposal: labeling, movement between compartments, and end‑of‑life policies. Implemented in `/docs/wiki/utxo-lifecycle-audit.md`. | Medium | Related: `provenance-labeling`, `utxo-segregation-playbook`. |
| ✅ | wiki `watch-only-restoration` | Reliable watch‑only restores with descriptors: rescans, gap limits, and verification steps. Implemented in `/docs/wiki/watch-only-restoration.md`. | High | Related: `descriptors-advanced`; Glossary: `gap-limit`, `descriptor`. |
| ✅ | wiki `psbt-airgap-basics` | PSBT‑first workflows for air‑gapped signing devices; metadata, QR transfer, and validation. Implemented in `/docs/wiki/psbt-airgap-basics.md`. | Medium | Related: `descriptors-advanced`; Glossary: `psbt`. |
| ✅ | wiki `bech32-vs-bech32m` | Address encoding primer, error detection, and script mapping (SegWit v0 vs. Taproot). Implemented in `/docs/wiki/bech32-vs-bech32m.md`. | Medium | Related: `address-format-fingerprints`; Glossary: `bech32`, `bech32m`. |
| ✅ | wiki `taproot-privacy-model` | Key‑path vs. script‑path spending, what leaks on‑chain, and safe defaults for monitoring. Implemented in `/docs/wiki/taproot-privacy-model.md`. | Medium | Related: `script-type-tradeoffs`; Glossary: `key-path`, `script-path`. |
| ✅ | wiki `policy-risk-register` | Catalog of wallet policy pitfalls (timelocks, non‑standard scripts, dust creation) and how to avoid them. Implemented in `/docs/wiki/policy-risk-register.md`. | Medium | Related: `transaction-standardness-vs-consensus`. |
| ✅ | glossary `mempool` | Set of unconfirmed transactions held by nodes; policies vary; link to mempool variance article. Implemented in `/docs/glossary/mempool.md`. | Medium | References `mempool-variance-and-policy`. |
| ✅ | glossary `feerate` | Sats/vbyte concept, target selection, and links to fee selection guide. Implemented in `/docs/glossary/feerate.md`. | Medium | References `fee-selection-playbook`. |
| ✅ | glossary `key-path` | Taproot key‑path spending; mention the privacy angle; link to the taproot article. Implemented in `/docs/glossary/key-path.md`. | Low | References `taproot-privacy-model`. |
| ✅ | glossary `script-path` | Taproot script‑path spending; when it reveals conditions; link to the taproot article. Implemented in `/docs/glossary/script-path.md`. | Low | References `taproot-privacy-model`. |
| ✅ | wiki `watch-only-threat-model` | Clarify what watch‑only protects (no keys, monitoring only) and what it does not (device compromise, malicious servers). Provide assumptions and safe defaults. Implemented in `/docs/wiki/watch-only-threat-model.md`. | Medium | Related: `node-trust-model`, `descriptors-advanced`. |
| ✅ | wiki `label-export-bip329-workflows` | End‑to‑end workflows to export, merge, and round‑trip BIP‑329 labels across apps without leaking metadata. Implemented in `/docs/wiki/label-export-bip329-workflows.md`. | Medium | Related: `provenance-labeling`; Glossary: `bip-329`. |
| ✅ | wiki `consolidation-strategy` | How to plan consolidation windows by mempool conditions, minimize fingerprints, and prepare for future spends. Implemented in `/docs/wiki/consolidation-strategy.md`. | Medium | Related: `fee-selection-playbook`, `utxo-segregation-playbook`; Glossary: `consolidation`. |
| ✅ | wiki `utxo-selection-heuristics` | Overview of input selection strategies (knapsack, branch‑and‑bound) and privacy‑aware constraints for real spends. Implemented in `/docs/wiki/utxo-selection-heuristics.md`. | Medium | Related: `fee-selection-playbook`; Glossary: `knapsack`, `branch-and-bound`. |
| ✅ | wiki `multisig-portability` | Descriptor‑based multisig portability, cosigner maps, policy descriptors, and recovery drills for teams. Implemented in `/docs/wiki/multisig-portability.md`. | Medium | Related: `descriptors-advanced`; Glossary: `cosigner`, `policy-descriptor`. |
| ✅ | wiki `bip389-multipath-practical` | Practical guidance for BIP‑389 multipath descriptors, common pitfalls (branch order) and validation tips. Implemented in `/docs/wiki/bip389-multipath-practical.md`. | Medium | Related: `descriptors-advanced`; Glossary: `multipath`. |
| ✅ | wiki `address-discovery-and-gap-limit` | Tuning discovery and gap limits for watch‑only scanning; performance vs coverage tradeoffs. Implemented in `/docs/wiki/address-discovery-and-gap-limit.md`. | Medium | Related: `watch-only-restoration`; Glossary: `gap-limit`. |
| ✅ | wiki `tor-hardening-for-nodes` | Bridges, pluggable transports, and network hygiene to keep node traffic private in restrictive environments. Implemented in `/docs/wiki/tor-hardening-for-nodes.md`. | Low | Related: `node-trust-model`; Glossary: `bridge`. |
| ✅ | wiki `bip78-payjoin-overview` | BIP‑78 PayJoin basics tailored for privacy: how interactive input contribution breaks CIOH, how to detect/verify PayJoin spends, and what watch‑only users can prepare (labels, candidate inputs) before PSBT export to a PayJoin‑capable signer. Implemented in `/docs/wiki/bip78-payjoin-overview.md`. | Medium | Related: `rbf-cpfp-strategies`, `utxo-selection-heuristics`; Glossary: `payjoin`, `cioh`. |
| ✅ | glossary `payjoin` | Interactive spend protocol (BIP‑78) where the receiver contributes inputs to obscure ownership and amounts; watch‑only impact and detection notes. Implemented in `/docs/glossary/payjoin.md`. | Medium | References `bip78-payjoin-overview`. |
| ✅ | glossary `cioh` | Common‑Input Ownership Heuristic: assumption that all inputs belong to one entity; why merges leak identity; methods that break it. Implemented in `/docs/glossary/cioh.md`. | Medium | References `utxo-segregation-playbook`, `bip78-payjoin-overview`. |
| ✅ | wiki `peel-chain-patterns` | How peel chains arise from repeated change reuse; how they leak clusters over time; mitigations (change quarantine, compartment rules, scheduled consolidation). Implemented in `/docs/wiki/peel-chain-patterns.md`. | Medium | Related: `change-output-hygiene`, `consolidation-strategy`; Glossary: `peel-chain`. |
| ✅ | glossary `peel-chain` | A transaction pattern where a wallet repeatedly spends from one UTXO and peels change, creating a linkable chain. Implemented in `/docs/glossary/peel-chain.md`. | Low | References `peel-chain-patterns`. |
| ✅ | wiki `electrum-servers` | Electrum server trust and privacy: why self‑hosting behind Tor matters, public server data exposure, server rotation risks, and baseline monitoring. Implemented in `/docs/wiki/electrum-servers.md`. | High | Related: `node-trust-model`, `why-tor`; Glossary: `electrum-server`. |
| ✅ | glossary `electrum-server` | Indexing service for wallet queries; privacy tradeoffs vs. full validation; best practice is self‑hosted over Tor. Implemented in `/docs/glossary/electrum-server.md`. | Medium | References `electrum-servers`. |
| ✅ | wiki `amount-analysis-heuristics` | What output amounts can reveal (exact purchase prices, round figures, dust), and how to minimize amount‑based fingerprinting in drafts. Implemented in `/docs/wiki/amount-analysis-heuristics.md`. | Low | Related: `fee-selection-playbook`, `change-output-hygiene`; Glossary: `dust`. |
| ✅ | glossary `knapsack` | Coin selection approach; tradeoffs vs branch‑and‑bound; link to selection heuristics article. Implemented in `/docs/glossary/knapsack.md`. | Low | References `utxo-selection-heuristics`. |
| ✅ | glossary `branch-and-bound` | Coin selection approach that aims to hit target values exactly; link to selection heuristics article. Implemented in `/docs/glossary/branch-and-bound.md`. | Low | References `utxo-selection-heuristics`. |
| ✅ | glossary `consolidation` | Merging smaller UTXOs into larger ones to reduce future fees; risky if it links compartments. Implemented in `/docs/glossary/consolidation.md`. | Medium | References `consolidation-strategy`. |
| ✅ | glossary `psbt` | Container format for Partially Signed Bitcoin Transactions used to coordinate inputs, outputs, and signing metadata across devices and apps; watch-only wallets create and export PSBTs without private keys. Implemented in `/docs/glossary/psbt.md`. | High | References `utxo-selection-heuristics`, `rbf-cpfp-strategies`. |
| ✅ | glossary `xpub` | Extended public key that derives a branch of addresses without revealing private keys; essential for watch-only descriptors and safe imports with correct origin/path. Implemented in `/docs/glossary/xpub.md`. | High | References `watch-only-restoration`, `descriptors-advanced`. |
| ✅ | glossary `key-fingerprint` | Short identifier (BIP32 fingerprint) for a master key used in descriptors and PSBT origin data to map keys/cosigners reliably. Implemented in `/docs/glossary/key-fingerprint.md`. | Medium | References `descriptor-maps-and-recovery`, `descriptors-advanced`. |
| ✅ | glossary `derivation-path` | Notation that specifies how keys/addresses are derived (e.g., m/84'/0'/0'/0/0); correct paths are required for accurate watch-only scanning and recovery. Implemented in `/docs/glossary/derivation-path.md`. | Medium | References `watch-only-restoration`, `descriptors-advanced`. |
| ✅ | glossary `miniscript` | Structured language for expressing spending policies that compile to standard Bitcoin Script; improves analysis and safety for policy descriptors. Implemented in `/docs/glossary/miniscript.md`. | Medium | References `descriptors-advanced`. |
| ✅ | glossary `policy-descriptor` | Descriptor that encodes a spending policy (often via Miniscript), enabling wallets to derive scripts and validate conditions deterministically. Implemented in `/docs/glossary/policy-descriptor.md`. | Medium | References `descriptors-advanced`. |
| ✅ | glossary `multipath` | BIP‑389 pattern that expresses external and change branches in a single descriptor (e.g., `/<0;1>/*`); clarifies scanning and export for watch-only wallets. Implemented in `/docs/glossary/multipath.md`. | Medium | References `watch-only-restoration`, `descriptors-advanced`. |
| ✅ | glossary `dust` | Output whose value is near or below the cost to spend; often pruned by standardness rules and a source of fingerprinting if created repeatedly. Implemented in `/docs/glossary/dust.md`. | Low | References `fee-selection-playbook`, `change-output-hygiene`. |
| ✅ | glossary `anonymity-set` | The number of plausible owners a transaction or output could belong to; larger sets improve privacy, smaller sets leak patterns. Implemented in `/docs/glossary/anonymity-set.md`. | Low | References `utxo-segregation-playbook`, `utxo-selection-heuristics`. |
| ✅ | glossary `coinjoin` | Collaborative transaction where multiple participants contribute inputs to break ownership heuristics; watch-only wallets can label and monitor post-mix UTXOs. Implemented in `/docs/glossary/coinjoin.md`. | Medium | References `utxo-segregation-playbook`, `script-type-tradeoffs`. |
| ✅ | glossary `spv` | Simplified Payment Verification: lightweight verification model that trusts peers for transaction inclusion; weaker privacy/trust than running your own node or querying your own Electrum server over Tor. Implemented in `/docs/glossary/spv.md`. | Medium | References `node-trust-model`, `electrum-servers`. |
| ✅ | glossary `outpoint` | A specific spendable reference identified by `txid:vout`; the pointer to a UTXO that becomes a transaction input. Implemented in `/docs/glossary/outpoint.md`. | Medium | References `utxo-selection-heuristics`. |
| ✅ | glossary `taproot` | Upgrade enabling key- and script-path spending with improved privacy and efficiency; hides script conditions unless script-path is used. Implemented in `/docs/glossary/taproot.md`. | Medium | References `script-type-tradeoffs`, `descriptors-advanced`. |
| ✅ | glossary `watch-only` | A wallet that holds only public data (descriptors, xpubs, addresses) to monitor balances and draft PSBTs, without any signing keys. Implemented in `/docs/glossary/watch-only.md`. | High | References `watch-only-restoration`, `descriptor-maps-and-recovery`. |
| ✅ | glossary `multisig` | A policy requiring M-of-N keys to spend; expressed via descriptors like `wsh(sortedmulti(...))` and coordinated with PSBTs. Implemented in `/docs/glossary/multisig.md`. | Medium | References `descriptors-advanced`. |
| ✅ | glossary `cosigner` | A participant holding one of the keys in a multisig policy; identified via key fingerprints and origins in descriptors/PSBTs. Implemented in `/docs/glossary/cosigner.md`. | Medium | References `descriptor-maps-and-recovery`, `descriptors-advanced`. |
| ✅ | glossary `sortedmulti` | Descriptor function that enforces lexicographic key ordering for multisig, avoiding malleability and improving compatibility. Implemented in `/docs/glossary/sortedmulti.md`. | Medium | References `descriptors-advanced`. |
| ✅ | glossary `key-origin` | BIP32 origin data (fingerprint and derivation path) embedded in descriptors/PSBTs to bind keys to their source and avoid mixups. Implemented in `/docs/glossary/key-origin.md`. | Medium | References `descriptor-maps-and-recovery`, `descriptors-advanced`. |
| ✅ | glossary `vbytes` | Virtual byte unit used for fee calculation (weight/4, rounded); feerates are expressed in sats/vbyte. Implemented in `/docs/glossary/vbytes.md`. | Medium | References `fee-selection-playbook`, `rbf-cpfp-strategies`. |
| ✅ | glossary `bridge` | Tor bridge or pluggable transport used to reach the Tor network from censored networks; helps hide node traffic and bootstrapping. Implemented in `/docs/glossary/bridge.md`. | Low | References `node-connectivity`, `electrum-servers`. |
| ✅ | wiki `encrypted-watch-only-backup` | Operational guide for encrypted `.ubak` export/import, scope boundaries, preview checks, and fail-closed behavior. Implemented in `/docs/wiki/encrypted-watch-only-backup.md`. | High | Related: `watch-only-restoration`, `descriptor-maps-and-recovery`; Glossary: `encrypted-backup`, `backup-passphrase`, `backup-integrity`. |
| ✅ | wiki `backup-recovery-drill` | Repeatable dry-run checklist to validate encrypted backup recovery before incidents. Implemented in `/docs/wiki/backup-recovery-drill.md`. | High | Related: `encrypted-watch-only-backup`, `self-custody-hygiene`; Glossary: `backup-integrity`, `backup-passphrase`. |
| ✅ | glossary `encrypted-backup` | Definition of UtxoPocket encrypted `.ubak` backup scope and purpose. Implemented in `/docs/glossary/encrypted-backup.md`. | Medium | References `encrypted-watch-only-backup`. |
| ✅ | glossary `backup-passphrase` | Definition and storage guidance for the passphrase required to unlock `.ubak` backups. Implemented in `/docs/glossary/backup-passphrase.md`. | Medium | References `encrypted-watch-only-backup`, `backup-recovery-drill`. |
| ✅ | glossary `backup-integrity` | Definition of integrity checks before import (preview, schema/security validation, expected metadata). Implemented in `/docs/glossary/backup-integrity.md`. | Medium | References `encrypted-watch-only-backup`, `descriptor-maps-and-recovery`. |

| ✅ | wiki `address-and-uri-standards` | Address and `bitcoin:` URI validation and sharing guidance. Implemented in `/docs/wiki/address-and-uri-standards.md`. | Medium | Related: `address-formats`, `tx-anatomy`; Glossary: `bech32`, `bech32m`. |
| ✅ | wiki `address-formats` | Practical comparison of legacy, SegWit, and Taproot address families. Implemented in `/docs/wiki/address-formats.md`. | Medium | Related: `address-format-fingerprints`, `transaction-fees`; Glossary: `bech32`, `bech32m`. |
| ✅ | wiki `backup-recovery` | Backup/recovery foundations and repeatable recovery process notes. Implemented in `/docs/wiki/backup-recovery.md`. | High | Related: `encrypted-watch-only-backup`, `backup-recovery-drill`; Glossary: `encrypted-backup`, `backup-integrity`. |
| ✅ | wiki `bitcoin-dev-kit` | BDK role in watch-only descriptor wallets and sync flows. Implemented in `/docs/wiki/bitcoin-dev-kit.md`. | Medium | Related: `descriptors-101`, `wallet-syncing`; Glossary: `descriptor`, `psbt`. |
| ✅ | wiki `bitcoin-future-tech` | Emerging protocol/policy directions evaluated for watch-only operators. Implemented in `/docs/wiki/bitcoin-future-tech.md`. | Low | Related: `miniscript`, `spending-policies`; Glossary: `taproot`, `policy-descriptor`. |
| ✅ | wiki `bitcoin-networking` | Node relay and wallet metadata exposure basics. Implemented in `/docs/wiki/bitcoin-networking.md`. | Medium | Related: `node-connectivity`, `why-tor`; Glossary: `full-node`, `spv`. |
| ✅ | wiki `bitcoin-privacy` | Core privacy model and common linkage failures. Implemented in `/docs/wiki/bitcoin-privacy.md`. | High | Related: `utxo-segregation-playbook`, `labeling-metadata`; Glossary: `anonymity-set`, `coin-control`. |
| ✅ | wiki `block-and-pow` | Block ordering, proof-of-work security, and confirmation risk framing. Implemented in `/docs/wiki/block-and-pow.md`. | Medium | Related: `confirmation-policy`, `mempool-fees`; Glossary: `confirmation`. |
| ✅ | wiki `coin-control` | Input selection control patterns for privacy/policy boundaries. Implemented in `/docs/wiki/coin-control.md`. | High | Related: `utxo-selection-heuristics`, `labeling-metadata`; Glossary: `coin-control`, `utxo`. |
| ✅ | wiki `coin-selection-algos` | Practical behavior of knapsack/branch-and-bound style selectors. Implemented in `/docs/wiki/coin-selection-algos.md`. | Medium | Related: `coin-control`, `transaction-fees`; Glossary: `knapsack`, `branch-and-bound`. |
| ✅ | wiki `descriptors-101` | Introductory descriptor structure, checksums, and branch mapping. Implemented in `/docs/wiki/descriptors-101.md`. | High | Related: `descriptors-advanced`, `watch-only-restoration`; Glossary: `descriptor`, `descriptor-checksum`. |
| ✅ | wiki `hd-derivation` | HD tree/path basics and watch-only recovery correctness. Implemented in `/docs/wiki/hd-derivation.md`. | Medium | Related: `keys-and-seeds`, `address-discovery-and-gap-limit`; Glossary: `derivation-path`, `xpub`. |
| ✅ | wiki `keys-and-seeds` | Key/seed role separation in watch-only architecture. Implemented in `/docs/wiki/keys-and-seeds.md`. | High | Related: `wallet-types`, `transaction-signing`; Glossary: `seed-phrase`, `watch-only`. |
| ✅ | wiki `labeling-metadata` | Label schema discipline and metadata lifecycle guidance. Implemented in `/docs/wiki/labeling-metadata.md`. | High | Related: `provenance-labeling`, `label-export-bip329-workflows`; Glossary: `bip-329`, `compartment`. |
| ✅ | wiki `mempool-fees` | Mempool congestion patterns and fee-pressure tactics. Implemented in `/docs/wiki/mempool-fees.md`. | Medium | Related: `transaction-fees`, `rbf-cpfp`; Glossary: `mempool`, `feerate`. |
| ✅ | wiki `miniscript` | Miniscript policy model and watch-only analysis value. Implemented in `/docs/wiki/miniscript.md`. | Medium | Related: `spending-policies`, `descriptors-advanced`; Glossary: `miniscript`, `policy-descriptor`. |
| ✅ | wiki `operational-hygiene` | Repeatable wallet operations checklist for reliability/privacy. Implemented in `/docs/wiki/operational-hygiene.md`. | High | Related: `operational-security`, `backup-recovery`; Glossary: `backup-integrity`, `tor`. |
| ✅ | wiki `operational-security` | Threat-driven opsec controls for watch-only flows. Implemented in `/docs/wiki/operational-security.md`. | High | Related: `watch-only-threat-model`, `why-tor`; Glossary: `watch-only`, `tor`. |
| ✅ | wiki `psbt-explained` | PSBT workflow from draft review to isolated signing. Implemented in `/docs/wiki/psbt-explained.md`. | High | Related: `psbt-airgap-basics`, `transaction-signing`; Glossary: `psbt`, `key-fingerprint`. |
| ✅ | wiki `rbf-cpfp` | Fundamental fee-bump patterns and selection guidance. Implemented in `/docs/wiki/rbf-cpfp.md`. | Medium | Related: `rbf-cpfp-strategies`, `mempool-fees`; Glossary: `rbf`, `cpfp`. |
| ✅ | wiki `spending-policies` | Policy matrices for approvals, confirmations, and boundaries. Implemented in `/docs/wiki/spending-policies.md`. | Medium | Related: `confirmation-policy`, `coin-control`; Glossary: `policy-descriptor`, `confirmation`. |
| ✅ | wiki `testnet-faucets` | Responsible faucet usage for wallet QA drills. Implemented in `/docs/wiki/testnet-faucets.md`. | Low | Related: `testnet-regtest`, `wallet-syncing`; Glossary: `mempool`, `confirmation`. |
| ✅ | wiki `testnet-regtest` | Practical comparison of testnet and regtest test loops. Implemented in `/docs/wiki/testnet-regtest.md`. | Low | Related: `testnet-faucets`, `wallet-syncing`; Glossary: `full-node`, `confirmation`. |
| ✅ | wiki `tor-integration` | Tor routing requirements and fail-closed sync behavior. Implemented in `/docs/wiki/tor-integration.md`. | High | Related: `why-tor`, `tor-vs-vpn`; Glossary: `tor`, `bridge`. |
| ✅ | wiki `tor-vs-vpn` | Transport threat-model comparison for wallet metadata privacy. Implemented in `/docs/wiki/tor-vs-vpn.md`. | Medium | Related: `why-tor`, `bitcoin-networking`; Glossary: `tor`. |
| ✅ | wiki `transaction-fees` | Fee mechanics by weight/feerate and urgency. Implemented in `/docs/wiki/transaction-fees.md`. | Medium | Related: `mempool-fees`, `tx-anatomy`; Glossary: `feerate`, `vbytes`. |
| ✅ | wiki `transaction-signing` | Secure signing lifecycle with watch-only review gates. Implemented in `/docs/wiki/transaction-signing.md`. | High | Related: `psbt-explained`, `keys-and-seeds`; Glossary: `psbt`, `seed-phrase`. |
| ✅ | wiki `tx-anatomy` | Inputs/outputs/change structure for spend review. Implemented in `/docs/wiki/tx-anatomy.md`. | Medium | Related: `utxo-basics`, `transaction-fees`; Glossary: `utxo`, `outpoint`. |
| ✅ | wiki `utxo-basics` | UTXO model essentials and operational implications. Implemented in `/docs/wiki/utxo-basics.md`. | High | Related: `tx-anatomy`, `coin-control`; Glossary: `utxo`, `change-output`. |
| ✅ | wiki `utxopocket-overview` | Canonical product posture summary (watch-only, Tor default, no telemetry). Implemented in `/docs/wiki/utxopocket-overview.md`. | High | Related: `watch-only-threat-model`, `tor-integration`; Glossary: `watch-only`, `tor`. |
| ✅ | wiki `wallet-syncing` | Descriptor discovery and backend sync behavior overview. Implemented in `/docs/wiki/wallet-syncing.md`. | High | Related: `address-discovery-and-gap-limit`, `watch-only-restoration`; Glossary: `gap-limit`, `full-rescan`. |
| ✅ | wiki `wallet-types` | Trust-boundary comparison across wallet models. Implemented in `/docs/wiki/wallet-types.md`. | Medium | Related: `keys-and-seeds`, `transaction-signing`; Glossary: `watch-only`, `psbt`. |
| ✅ | wiki `why-tor` | Privacy rationale for Tor transport in wallet operations. Implemented in `/docs/wiki/why-tor.md`. | High | Related: `tor-integration`, `electrum-servers`; Glossary: `tor`, `electrum-server`. |

| ⏳ | wiki `bip340-schnorr-overview` | Explain Schnorr signatures and practical implications for multisig coordination, fee efficiency, and script privacy tradeoffs. Pending. | Medium | Related: `taproot-privacy-model`, `transaction-signing`; Glossary: `taproot`, `script-path`. |
| ⏳ | wiki `timelocks-and-cltv-csv` | Intro to absolute/relative timelocks in spending policies, with watch-only review checkpoints. Pending. | Medium | Related: `spending-policies`, `policy-risk-register`; Glossary: `script`, `confirmation`. |
| ⏳ | wiki `versionbits-softfork-signaling` | How miner signaling and activation windows work, and what wallet operators should monitor. Pending. | Low | Related: `bitcoin-future-tech`, `block-and-pow`; Glossary: `full-node`. |
| ⏳ | wiki `compact-block-filters-basics` | Explain compact block filters and lightweight client verification tradeoffs. Pending. | Medium | Related: `bitcoin-networking`, `node-trust-model`; Glossary: `spv`, `full-node`. |
| ⏳ | glossary `schnorr-signature` | Definition of BIP340 Schnorr signatures, aggregation context, and wallet interoperability notes. Pending. | Medium | References `bip340-schnorr-overview`, `taproot-privacy-model`. |
| ⏳ | glossary `cltv` | Definition of CheckLockTimeVerify and policy use in delayed spending conditions. Pending. | Medium | References `timelocks-and-cltv-csv`, `spending-policies`. |
| ⏳ | glossary `csv` | Definition of CheckSequenceVerify and relative timelock behavior in policy scripts. Pending. | Medium | References `timelocks-and-cltv-csv`, `spending-policies`. |
| ⏳ | glossary `versionbits` | Definition of versionbits signaling and activation mechanics for soft forks. Pending. | Low | References `versionbits-softfork-signaling`, `bitcoin-future-tech`. |
