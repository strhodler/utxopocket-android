package com.strhodler.utxopocket.presentation.wiki

data class WikiCategory(
    val id: String,
    val title: String,
    val description: String,
    val topics: List<WikiTopic>
)

data class WikiTopic(
    val id: String,
    val title: String,
    val summary: String,
    val sections: List<WikiSection>,
    val keywords: List<String> = emptyList(),
    val relatedTopicIds: List<String> = emptyList(),
    val glossaryRefIds: List<String> = emptyList()
)

data class WikiSection(
    val title: String?,
    val paragraphs: List<String>
)

private val wikiTopicKeywords: Map<String, List<String>> = mapOf(
    "utxo-basics" to listOf(
        "Unspent outputs",
        "Coin control",
        "Change management",
        "UTXO set",
        "Coinset",
        "Transaction inputs",
        "Change output",
        "Coin hygiene"
    ),
    "wallet-types" to listOf(
        "Single-sig",
        "Multi-sig",
        "Hardware wallets",
        "Descriptor wallets",
        "Hot wallet",
        "Cold storage",
        "Single key",
        "Multisig",
        "Watch-only"
    ),
    "keys-and-seeds" to listOf(
        "BIP39",
        "Mnemonic",
        "Private keys",
        "Derivation paths",
        "Entropy",
        "Seed backup",
        "Hardware seed",
        "BIP39 passphrase"
    ),
    "address-formats" to listOf(
        "SegWit",
        "Taproot",
        "Legacy addresses",
        "Bech32",
        "P2PKH",
        "P2SH",
        "Bech32m",
        "BC1 addresses"
    ),
    "backup-recovery" to listOf(
        "Seed backup",
        "Redundancy",
        "Passphrases",
        "Disaster recovery"
    ),
    "bitcoin-privacy" to listOf(
        "Privacy",
        "CoinJoin",
        "Address reuse",
        "Surveillance resistance",
        "Heuristics",
        "Address clustering",
        "Chain surveillance",
        "Privacy score"
    ),
    "privacy-threat-models" to listOf(
        "Threat modeling",
        "Metadata leaks",
        "Adversaries",
        "Privacy posture"
    ),
    "operational-hygiene" to listOf(
        "Best practices",
        "Device security",
        "Updates",
        "Phishing"
    ),
    "why-tor" to listOf(
        "Tor network",
        "Censorship resistance",
        "IP privacy",
        "Routing"
    ),
    "tor-integration" to listOf(
        "Tor configuration",
        "SOCKS5",
        "Network routing",
        "Bridges"
    ),
    "tor-vs-vpn" to listOf(
        "VPN comparison",
        "Privacy trade-offs",
        "Traffic analysis",
        "Tunnels"
    ),
    "bitcoin-networking" to listOf(
        "P2P network",
        "Nodes",
        "Propagation",
        "Peer connections"
    ),
    "transaction-fees" to listOf(
        "Fee estimation",
        "Feerate",
        "Sat per vbyte",
        "Mempool",
        "Replace-by-fee",
        "Child pays for parent",
        "Fee bumping"
    ),
    "mempool-fees" to listOf(
        "Mempool",
        "Fee market",
        "Congestion",
        "Backlog",
        "Feerate",
        "Sat/vB",
        "Replace-by-fee",
        "Child pays for parent"
    ),
    "wallet-syncing" to listOf(
        "Initial sync",
        "Block filters",
        "Headers",
        "Rescan"
    ),
    "electrum-servers" to listOf(
        "Server selection",
        "Self-hosted",
        "Tor connectivity",
        "SSL"
    ),
    "node-connectivity" to listOf(
        "Tor",
        "Electrum",
        "Node health",
        "Onion services",
        "Connectivity"
    ),
    "bitcoin-dev-kit" to listOf(
        "BDK",
        "Wallet toolkit",
        "Rust",
        "Descriptor wallets"
    ),
    "psbt-explained" to listOf(
        "Partially signed",
        "Multisig",
        "Hardware wallets",
        "Air-gapped"
    ),
    "transaction-signing" to listOf(
        "Signing flow",
        "Private keys",
        "Hardware wallet",
        "PSBT"
    ),
    "coin-control" to listOf(
        "Manual selection",
        "Privacy",
        "Dust management",
        "Fee control"
    ),
    "labeling-metadata" to listOf(
        "Transaction labels",
        "Notes",
        "Audit trail",
        "Context"
    ),
    "block-and-pow" to listOf(
        "Mining",
        "Proof of work",
        "Difficulty",
        "Hash rate"
    ),
    "tx-anatomy" to listOf(
        "Transaction structure",
        "Inputs",
        "Outputs",
        "Scripts"
    ),
    "hd-derivation" to listOf(
        "BIP32",
        "Xpubs",
        "Derivation paths",
        "Accounts"
    ),
    "address-and-uri-standards" to listOf(
        "BIP21",
        "URI",
        "Payment links",
        "QR codes"
    ),
    "rbf-cpfp" to listOf(
        "Replacement",
        "Fee bumping",
        "RBF",
        "CPFP"
    ),
    "coin-selection-algos" to listOf(
        "Branch and bound",
        "Knapsack",
        "Coin selection",
        "Spend strategy",
        "Random draw",
        "Efficient frontier"
    ),
    "descriptors-101" to listOf(
        "Output descriptors",
        "Policy",
        "Watch-only",
        "Scripts",
        "Descriptor templates",
        "Policy descriptors"
    ),
    "miniscript" to listOf(
        "Policy language",
        "Script templates",
        "Safety checks",
        "Descriptors",
        "Script miniscript",
        "Threshold",
        "Timelock"
    ),
    "spending-policies" to listOf(
        "Multisig",
        "Timelocks",
        "Policy controls",
        "Miniscript"
    ),
    "transaction-health" to listOf(
        "Transaction scoring",
        "Health badges",
        "Diagnostics",
        "Indicators",
        "Health score",
        "Risk assessment",
        "Alerts"
    ),
    "wallet-health" to listOf(
        "Wallet score",
        "Category breakdown",
        "Risk signals",
        "Monitoring",
        "Health score",
        "Optimization",
        "Alerts"
    ),
    "utxo-health" to listOf(
        "UTXO score",
        "Dust risk",
        "Consolidation",
        "Coin hygiene",
        "Consolidation risk",
        "Dust"
    ),
    "operational-security" to listOf(
        "OpSec",
        "Threat mitigation",
        "Information leaks",
        "Compartmentalization"
    ),
    "testnet-regtest" to listOf(
        "Test networks",
        "Regtest",
        "Development",
        "Sandbox",
        "Developer network",
        "Regtest automation",
        "Integration testing"
    ),
    "testnet-faucets" to listOf(
        "Testnet coins",
        "Faucet",
        "Developer tooling",
        "Sample funds",
        "Developer network",
        "Integration testing"
    ),
    "bitcoin-future-tech" to listOf(
        "Taproot",
        "Covenants",
        "Layer two",
        "Innovation",
        "Lightning",
        "Channel management",
        "Fedimint",
        "Ark",
        "Auction slots"
    )
)

object WikiContent {

    const val TransactionHealthTopicId: String = "transaction-health"
    const val UtxoHealthTopicId: String = "utxo-health"
    const val WalletHealthTopicId: String = "wallet-health"
    const val NodeConnectivityTopicId: String = "node-connectivity"
    const val DescriptorCompatibilityTopicId: String = "descriptor-compatibility"

    val categories: List<WikiCategory> = listOf(
        WikiCategory(
            id = "bitcoin-basics",
            title = "Bitcoin Basics",
            description = "Fundamental concepts to understand how Bitcoin wallets and the UTXO model work.",
            topics = listOf(
                WikiTopic(
                    id = "utxo-basics",
                    title = "UTXO Basics",
                    summary = "Learn what unspent outputs are and why they matter.",
                    sections = listOf(
                        WikiSection(
                            title = "How UTXOs Work",
                            paragraphs = listOf(
                                "Bitcoin tracks value using Unspent Transaction Outputs (UTXOs). Each time a transaction is confirmed, it creates new outputs that can later be spent, and consumes previous outputs as inputs. There is no concept of an account balance on-chain; your wallet balance is simply the sum of the UTXOs you control.",
                                "Because UTXOs are indivisible units, spending them typically generates change: if you spend less than the full value of a UTXO, your wallet creates a new output returning the remainder back to you. This has implications for fee calculation, privacy, and how wallets pick which coins to spend."
                            )
                        ),
                        WikiSection(
                            title = "Why It Matters",
                            paragraphs = listOf(
                                "Understanding UTXOs helps you interpret wallet behavior, especially when you see multiple entries for a single wallet or when fees seem higher than expected. Each additional input you include consumes space in the transaction and raises the fee.",
                                "Advanced workflows like coin control, batching payments, or leveraging privacy tools all rely on manipulating the underlying UTXOs. Knowing what is happening under the hood lets you choose the right strategy for consolidation, privacy, and cost."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "wallet-types",
                    title = "Wallet Types",
                    summary = "Single-sig, multi-sig, hardware, and descriptor-based wallets.",
                    sections = listOf(
                        WikiSection(
                            title = "Core Categories",
                            paragraphs = listOf(
                                "Single-signature wallets are the most common: one private key signs each transaction. They can run on mobile, desktop, or dedicated hardware. Multi-signature wallets require multiple keys to authorize spending, such as 2-of-3 setups that blend security and redundancy.",
                                "Hardware wallets keep signing keys on secure elements isolated from connected devices. Descriptor-based wallets describe the exact script template used for deriving addresses, enabling watch-only wallets like UtxoPocket to verify funds without holding private keys."
                            )
                        ),
                        WikiSection(
                            title = "Choosing the Right Fit",
                            paragraphs = listOf(
                                "Pick single-sig setups for everyday spending or smaller holdings, and multi-sig for collaborative custody, business treasury, or high-value vaults. Hardware devices add defense-in-depth by keeping keys offline.",
                                "Descriptor wallets shine when you need reproducibility across devices, want to enforce derivation paths, or plan to audit multi-sig setups. UtxoPocket leans on descriptors to monitor balances without exposing signing keys."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "keys-and-seeds",
                    title = "Keys and Seeds",
                    summary = "Understanding private keys, mnemonics, and derivation paths.",
                    sections = listOf(
                        WikiSection(
                            title = "From Seed to Keys",
                            paragraphs = listOf(
                                "A BIP39 mnemonic encodes 128–256 bits of entropy into 12–24 human-readable words. This seed feeds deterministic key derivation (BIP32/BIP44) to produce an entire hierarchy of private and public keys without storing each one individually.",
                                "Derivation paths like m/84'/0'/0'/0/0 describe how to reach a particular child key. Changing the path changes the resulting keys, so wallets must agree on standards (BIP44, BIP84, etc.) to remain interoperable."
                            )
                        ),
                        WikiSection(
                            title = "Safeguarding Secrets",
                            paragraphs = listOf(
                                "Never share your mnemonic or private keys. Anyone with access can spend your coins. Consider a passphrase (BIP39 extension word) to add an extra layer that protects against physical seed compromise.",
                                "Back up your mnemonic in durable form (metal, redundant copies) and test your recovery process before relying on it. Proper key hygiene is the foundation for every other security practice."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "address-formats",
                    title = "Address Formats",
                    summary = "Legacy, SegWit, and Taproot address types.",
                    sections = listOf(
                        WikiSection(
                            title = "Evolution of Formats",
                            paragraphs = listOf(
                                "Legacy addresses (P2PKH, starting with '1') were the original format but carry higher fees and limited features. P2SH (starting with '3') introduced script-based spending, enabling multi-signature and nested SegWit.",
                                "Native SegWit (bech32, starting with 'bc1q') reduces transaction weight and improves error detection, while Taproot (bech32m, starting with 'bc1p') unlocks key aggregation and advanced scripting with better privacy."
                            )
                        ),
                        WikiSection(
                            title = "Choosing an Address Type",
                            paragraphs = listOf(
                                "SegWit and Taproot addresses are cheaper to spend and more future-proof, but some services still rely on legacy formats. When interoperating with older counterparts, wallets may fall back to nested SegWit for compatibility.",
                                "For watch-only use, descriptors specify the exact script, ensuring the app derives addresses that match your signing wallet. Managing multiple formats in one wallet is normal—as long as you track which keys control which outputs."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "backup-recovery",
                    title = "Backup & Recovery",
                    summary = "How to safely back up and restore your wallet.",
                    sections = listOf(
                        WikiSection(
                            title = "Building a Durable Backup",
                            paragraphs = listOf(
                                "A complete backup captures your seed mnemonic, optional passphrase, and any metadata you rely on (labels, descriptors, multisig maps). Without all components, recovery may be partial or impossible.",
                                "Store backups in geographically separated locations and protect them from fire, water, and unauthorized access. Consider Shamir's Secret Sharing or multi-sig to balance redundancy and confidentiality."
                            )
                        ),
                        WikiSection(
                            title = "Testing Recovery",
                            paragraphs = listOf(
                                "Practice restoring in a controlled environment before disaster strikes. Verifying a backup catches transcription errors, missing passphrases, or assumptions about derivation paths.",
                                "Document the recovery steps you followed, noting software versions and configuration quirks. A reliable runbook helps future you—or your heirs—reconstruct access under stress."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "privacy-networking",
            title = "Privacy & Networking",
            description = "How to protect your privacy and understand how Bitcoin nodes communicate.",
            topics = listOf(
                WikiTopic(
                    id = "bitcoin-privacy",
                    title = "Bitcoin Privacy",
                    summary = "Common leaks and how to minimize them.",
                    sections = listOf(
                        WikiSection(
                            title = "Where Leaks Happen",
                            paragraphs = listOf(
                                "Privacy can break at the network layer (when your node reveals your IP), the transaction graph (address reuse, linking inputs), or the application layer (analytics, KYC, and metadata).",
                                "Mobile users are especially exposed when relying on third-party nodes or mixing personal and business funds in one wallet. Even labeling transactions poorly can reveal intent if your device is compromised."
                            )
                        ),
                        WikiSection(
                            title = "Mitigating Risks",
                            paragraphs = listOf(
                                "Route traffic through Tor, use fresh addresses for each receive, and segment funds by purpose. Coin control lets you avoid linking unrelated UTXOs, and batching can hide payment structure.",
                                "Consider collaborative transactions (CoinJoin, PayJoin) when practical, and minimize the personal data you hand to custodians or exchanges. Privacy is a posture, not a single toggle."
                            )
                        ),
                        WikiSection(
                            title = "Network-Level Defences",
                            paragraphs = listOf(
                                "If your wallet talks to public Electrum servers from your home connection, the IP can be linked to every derived address. Operating over Tor or a VPN you control, or querying your own node, makes simple IP clustering far harder.",
                                "Mobile users should harden Wi-Fi usage, prefer data connections, and consider Tor bridges or Pluggable Transports when a network filters standard Tor traffic. Timing analysis still exists, but you raise the bar considerably."
                            )
                        ),
                        WikiSection(
                            title = "On-Chain Fingerprints",
                            paragraphs = listOf(
                                "Heuristics such as common-input-ownership, script type transitions, and change address detection can cluster your activity. Keep separate descriptors for distinct roles (savings, bills, donations) to contain the blast radius of a leak.",
                                "Avoid consolidating coins just because the mempool is quiet. Large sweeps undo months of careful coin control. When you must consolidate, do it with new change paths and be mindful of surveillance firms that track sweeping behaviour."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = TransactionHealthTopicId,
                    title = "Transaction Health Overview",
                    summary = "Understand how UtxoPocket scores transactions and why badges matter.",
                    sections = listOf(
                        WikiSection(
                            title = "What Transaction Health Does",
                            paragraphs = listOf(
                                "Transaction Health runs locally on your device to analyse each transaction captured by your watch-only wallet. Inputs, outputs, and fee posture are inspected to highlight privacy leaks, inefficiencies, or dust-related risks without ever exposing signing keys.",
                                "The goal is to give you fast signal: does this transaction leak ownership clues, is it likely to get stuck, or should you plan a future consolidation instead? The analysis complements Wallet Health and UTXO Health so you can see how individual spends affect the bigger picture."
                            )
                        ),
                        WikiSection(
                            title = "Scoring Model",
                            paragraphs = listOf(
                                "Every transaction starts at 100 points. Indicators adjust the score across four pillars: Privacy (45%), Fees & Policy (25%), Efficiency (20%), and Risk Signals (10%). The final score is clamped between 0 and 100 so you can compare transactions at a glance.",
                                "Pillar breakdowns explain trade-offs. A spend might sacrifice some privacy (address reuse) but still excel on fee hygiene. Reviewing the pillars helps you decide which issues to prioritise before broadcasting new transactions."
                            )
                        ),
                        WikiSection(
                            title = "Indicators and Badges",
                            paragraphs = listOf(
                                "Indicators are individual heuristics—address reuse, identifiable change, dust spending, RBF posture, and more. Each indicator stores a score delta, severity, and evidence such as the output index or change ratio that triggered it.",
                                "Badges surface the most relevant findings so you can triage quickly. A warning like “Dust spent” points to hygiene tasks, while positive badges such as “Healthy posture” confirm you are following best practices. Future releases will let you filter the transaction list by these badges."
                            )
                        ),
                        WikiSection(
                            title = "Controls and Privacy",
                            paragraphs = listOf(
                                "Toggle Transaction Health in Settings → Privacy & analysis if you prefer a lighter interface or want to pause analysis. When disabled we skip computation, hide badges, and clear cached scores.",
                                "Because the system runs entirely on-device, no addresses, labels, or history are sent to external services. Re-enabling the feature recomputes scores from your stored wallet history so you can always rebuild the dataset."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = WalletHealthTopicId,
                    title = "Wallet Health Overview",
                    summary = "See how transaction and UTXO signals combine into a wallet-wide posture.",
                    sections = listOf(
                        WikiSection(
                            title = "Aggregating the Signals",
                            paragraphs = listOf(
                                "Wallet Health listens to Transaction Health and UTXO Health streams, blending their scores into four pillars: Privacy, Inventory, Efficiency, and Risk. The calculation stays on-device, so none of your labels or metrics leave UtxoPocket.",
                                "A single glance at the Wallet card shows whether your overall posture is trending up or down. Drill into pillars when you need to understand which area—hygiene, efficiency, or privacy—requires attention.",
                                "Need to inspect the raw heuristics? Jump into the Transaction Health Overview and UTXO Health Overview articles to see every indicator that feeds this aggregate score."
                            )
                        ),
                        WikiSection(
                            title = "Badges and Insights",
                            paragraphs = listOf(
                                "Badges highlight notable patterns such as elevated dust footprint or resilient privacy posture. Upcoming releases will surface next-step suggestions so you can act directly from the dashboard.",
                                "Snapshots are stored locally, enabling future timelines and alerts when the score drops below your target threshold."
                            )
                        ),
                        WikiSection(
                            title = "Controls and Toggles",
                            paragraphs = listOf(
                                "Wallet Health depends on both transaction and UTXO analysis. Enabling it from Settings automatically switches on those foundations and computes an initial snapshot.",
                                "If you disable either dependency, Wallet Health switches off and clears cached snapshots, ensuring you remain in control of what gets analysed."
                            )
                        )
                    ),
                    relatedTopicIds = listOf(TransactionHealthTopicId, UtxoHealthTopicId)
                ),
                WikiTopic(
                    id = UtxoHealthTopicId,
                    title = "UTXO Health Overview",
                    summary = "Keep your UTXO set tidy, private, and ready to spend.",
                    sections = listOf(
                        WikiSection(
                            title = "Why UTXO Health Matters",
                            paragraphs = listOf(
                                "Each UTXO is a mini output you might spend later. UTXO Health runs on-device to flag dust, address reuse, unlabeled change, and other hygiene issues that can undermine privacy or make future transactions expensive.",
                                "Keeping the set clean reduces fees, avoids poisoning patterns, and makes Wallet Health metrics more accurate. It is the foundation for reliable coin control."
                            )
                        ),
                        WikiSection(
                            title = "Scoring & Pillars",
                            paragraphs = listOf(
                                "UTXO Health starts at 100. Indicators adjust the score across four pillars borrowed from the implementation plan: Privacy (40%), Inventory Hygiene (30%), Availability (20%), and Risk Signals (10%).",
                                "The pillar breakdown highlights where to focus. If hygiene falls under 70, it is probably time to consolidate lingering change or dust outputs."
                            )
                        ),
                        WikiSection(
                            title = "Indicators and Badges",
                            paragraphs = listOf(
                                "Current indicators cover address reuse, dust pending, unconsolidated change, and missing labels. Each indicator records a delta, severity, and evidence such as value or confirmations.",
                                "Badges summarise the state (for example, 'Pending consolidation' or 'Dust pending'). Filtering by badges helps you prioritise cleanup before reusing outputs in new transactions."
                            )
                        ),
                        WikiSection(
                            title = "Controles y Privacidad",
                            paragraphs = listOf(
                                "Toggle UTXO Health in Settings → Privacy & analysis. When it is off we skip the heuristics and clear cached scores so you remain in control.",
                                "The analyser runs entirely on-device. Re-enabling the feature recalculates scores from your stored UTXOs without contacting any external service." 
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "privacy-threat-models",
                    title = "Threat Models",
                    summary = "Choose the right controls for the adversary you care about.",
                    sections = listOf(
                        WikiSection(
                            title = "Casual vs. Professional Adversaries",
                            paragraphs = listOf(
                                "Family members or co-workers are deterred by strong device locks, separate user profiles, and avoiding obvious address reuse. They rarely have the tools to deanonymise transaction flows.",
                                "Chain surveillance outfits combine leaked KYC data, social media breadcrumbs, and graph analytics. If that is your threat model, compartmentalise identities, prefer non-custodial services, and use collaborative transactions to blunt heuristics."
                            )
                        ),
                        WikiSection(
                            title = "State-Level Pressure",
                            paragraphs = listOf(
                                "Governments can compel service providers to hand over logs. Keep critical infrastructure (BTCPay, node, block explorer) under your control so subpoenas stop at your own perimeter.",
                                "When crossing borders, travel with watch-only descriptors and keep signing devices powered off. Memorise (or securely store separately) your passphrases so seizing one device does not grant full access."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "operational-hygiene",
                    title = "Operational Hygiene",
                    summary = "Practical habits distilled from well-known privacy guides.",
                    sections = listOf(
                        WikiSection(
                            title = "Segmentation & Documentation",
                            paragraphs = listOf(
                                "Map each descriptor to a purpose and record where its signing keys live. Shared runbooks reduce the risk of panicked mistakes during recovery and make audits easier.",
                                "When sharing data with advisors, export the minimum set of addresses needed. Watch-only descriptors are powerful, but oversharing can still leak internal budgeting details." 
                            )
                        ),
                        WikiSection(
                            title = "Collaborative Transactions",
                            paragraphs = listOf(
                                "CoinJoin, PayJoin, and Stonewall transactions change the statistical profile of your payments. They are not silver bullets but they increase the cost of surveillance.",
                                "Coordinate over secure channels (Matrix, Signal, SimpleX) so aborted rounds do not strand partially signed PSBTs. Always review outputs before broadcast to ensure change lands where you expect."
                            )
                        ),
                        WikiSection(
                            title = "Metadata Hygiene",
                            paragraphs = listOf(
                                "Turn off diagnostic uploads, crash reports, and clipboard syncing for wallets. Many mobile OS conveniences are privacy liabilities when tracking sensitive financial data.",
                                "Treat exported CSVs, descriptors, and screenshots as sensitive material—encrypt archives, scrub personal notes, and avoid copying wallet data into online productivity tools." 
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "why-tor",
                    title = "Why Tor?",
                    summary = "Privacy-first networking baseline.",
                    sections = listOf(
                        WikiSection(
                            title = "Protecting Network Metadata",
                            paragraphs = listOf(
                                "Tor hides your IP address by routing traffic through relays and exit nodes before reaching a Bitcoin server. Observers see Tor nodes communicating, not your home IP.",
                                "For watch-only wallets, Tor is a low-cost way to prevent the server from correlating queries with a physical location or ISP account. It is especially important when accessing public Electrum servers."
                            )
                        ),
                        WikiSection(
                            title = "Trade-offs and Constraints",
                            paragraphs = listOf(
                                "Tor introduces latency and occasional circuit instability. Wallets must cache data and handle retries gracefully to avoid degrading the user experience.",
                                "Some services block Tor exits. UtxoPocket sticks to Tor-first connectivity but can surface diagnostics so users understand when a backend is unreachable."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "tor-integration",
                    title = "Tor Integration",
                    summary = "How Tor is used inside the app for secure connections.",
                    sections = listOf(
                        WikiSection(
                            title = "Embedded Tor Client",
                            paragraphs = listOf(
                                "UtxoPocket bundles a Tor daemon and manages circuits directly on-device. The wallet routes Electrum or Esplora requests through a SOCKS proxy exposed by the Tor client.",
                                "This design avoids relying on system-wide Tor installations and gives the app control over circuit lifetime, isolation, and restart policies."
                            )
                        ),
                        WikiSection(
                            title = "User-Facing Experience",
                            paragraphs = listOf(
                                "The UI surfaces Tor status (connecting, running, error) so users know when network privacy is active. If Tor connectivity drops, syncing pauses rather than falling back to clearnet.",
                                "Advanced users can refresh identities or inspect logs for troubleshooting, while default flows stay automated for those who just want private watch-only monitoring."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "tor-vs-vpn",
                    title = "Tor vs VPN",
                    summary = "Why Tor is used instead of a VPN for privacy.",
                    sections = listOf(
                        WikiSection(
                            title = "Different Trust Models",
                            paragraphs = listOf(
                                "VPNs encrypt traffic between you and a VPN server, but that provider still sees your IP and destination requests. You must fully trust their logging policy.",
                                "Tor distributes trust across relays and exit nodes, meaning no single operator sees the full picture. Even if an exit is malicious, it cannot link activity back to your IP."
                            )
                        ),
                        WikiSection(
                            title = "Why Tor by Default",
                            paragraphs = listOf(
                                "Tor is open, decentralized, and battle-tested in hostile environments. It aligns with the threat model of Bitcoin users who value censorship resistance and anonymity.",
                                "VPNs can be useful for geofencing or bandwidth, but they are a single point of failure. UtxoPocket treats Tor as the baseline and leaves VPN usage as an optional, external layer."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = NodeConnectivityTopicId,
                    title = "Node Connectivity Checklist",
                    summary = "Keep Tor online and validate your backend before syncing descriptors.",
                    sections = listOf(
                        WikiSection(
                            title = "Tor Is Mandatory",
                            paragraphs = listOf(
                                "UtxoPocket bundles its own Tor daemon and refuses to add or switch nodes while Tor is offline. Wait for the status bar to show Tor as running before opening the node picker.",
                                "If Tor bootstrap stalls, renew your identity from the Tor status screen, toggle airplane mode, or resolve any captive portal before attempting another node change."
                            )
                        ),
                        WikiSection(
                            title = "Adding or Switching Nodes",
                            paragraphs = listOf(
                                "Use the Network settings screen to select a bundled Electrum backend or add your own onion endpoint. Provide the hostname, port, and certificate expectations so the app can verify the peer.",
                                "After changing nodes, pull to refresh on the home screen so descriptors rescan against the new backend. Expect temporary lock indicators while historical data replays."
                            )
                        ),
                        WikiSection(
                            title = "Quick Health Checks",
                            paragraphs = listOf(
                                "Compare the block height and fee rate shown in the top app bar with another trusted source. Large gaps suggest the backend is stale or misconfigured.",
                                "Maintain at least one fallback server. If an onion host goes dark, switching to a known-good peer keeps watch-only monitoring online without exposing clearnet metadata."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "bitcoin-networking",
                    title = "Bitcoin Networking",
                    summary = "How nodes communicate and why decentralization matters.",
                    sections = listOf(
                        WikiSection(
                            title = "Peer-to-Peer Gossip",
                            paragraphs = listOf(
                                "Bitcoin nodes form an unstructured mesh network using TCP. They exchange inventory messages to announce blocks and transactions, relying on peer diversity to propagate data quickly.",
                                "The protocol is bandwidth-efficient and resilient: if a peer misbehaves, nodes can drop it without affecting the rest of the network."
                            )
                        ),
                        WikiSection(
                            title = "Why Decentralization Matters",
                            paragraphs = listOf(
                                "When you rely on a single server, you inherit its censorship and outage risk. Running or selecting independent nodes distributes trust and keeps the system permissionless.",
                                "For watch-only wallets, choosing reputable Electrum or Esplora backends—and ideally running your own—protects you from tampered data or selective transaction relay."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "fees-mempool",
            title = "Fees & Mempool",
            description = "Learn how transaction fees work and how the Bitcoin mempool prioritizes transactions.",
            topics = listOf(
                WikiTopic(
                    id = "transaction-fees",
                    title = "Transaction Fees",
                    summary = "How Bitcoin fees work and how to optimize them.",
                    sections = listOf(
                        WikiSection(
                            title = "Fee Basics",
                            paragraphs = listOf(
                                "Fees are paid in satoshis per virtual byte (sat/vB). Miners prioritize transactions offering higher fee density because block space is scarce.",
                                "A transaction's weight depends on the number of inputs, outputs, and the script type. SegWit inputs weigh less than legacy ones, so upgrading your address format directly saves fees."
                            )
                        ),
                        WikiSection(
                            title = "Optimizing Costs",
                            paragraphs = listOf(
                                "Consolidate small UTXOs during quiet mempool periods, batch payments when possible, and enable Replace-By-Fee (RBF) to adjust if the market changes before confirmation.",
                                "Fee estimators use historical mempool data to predict the minimum viable rate for your target confirmation window. Monitoring mempool congestion lets you adapt strategies proactively."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "mempool-fees",
                    title = "Mempool & Fees",
                    summary = "Understand fee estimation and mempool dynamics.",
                    sections = listOf(
                        WikiSection(
                            title = "What the Mempool Represents",
                            paragraphs = listOf(
                                "Each node maintains a memory pool (mempool) of unconfirmed transactions it is willing to relay. There is no single global mempool, but well-connected nodes converge on similar sets.",
                                "Transactions compete for limited block space. Nodes drop low-fee transactions when the mempool hits size limits, so stale or underpriced transactions can disappear from peers."
                            )
                        ),
                        WikiSection(
                            title = "Reading Mempool Signals",
                            paragraphs = listOf(
                                "Fee estimation looks at how many transactions at each fee tier are likely to confirm within the next blocks. Spikes in usage can push recommended fees up quickly.",
                                "Monitoring mempool charts helps you decide when to wait, accelerate with RBF/CPFP, or restructure payments. UtxoPocket surfaces the current fee bands so you can choose deliberately."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "sync-backends",
            title = "Sync & Backends",
            description = "The engines and services that power wallet synchronization and blockchain queries.",
            topics = listOf(
                WikiTopic(
                    id = "wallet-syncing",
                    title = "Wallet Syncing",
                    summary = "How wallet synchronization works with Electrum or Esplora.",
                    sections = listOf(
                        WikiSection(
                            title = "Scanning for Activity",
                            paragraphs = listOf(
                                "Watch-only wallets derive addresses from descriptors and query backends for history. Electrum and Esplora support address lookups, balance checks, and transaction downloads.",
                                "Efficient syncing relies on gap limits: the wallet scans a window of unused addresses to detect new activity without probing the entire keyspace."
                            )
                        ),
                        WikiSection(
                            title = "Balancing Speed and Privacy",
                            paragraphs = listOf(
                                "Full nodes provide the strongest privacy but require local storage. Electrum servers and Esplora APIs trade some trust for convenience but can be accessed over Tor.",
                                "UtxoPocket caches confirmed data and uses incremental updates to minimize round trips, keeping the interface snappy even when Tor introduces latency."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "electrum-servers",
                    title = "Electrum Servers",
                    summary = "How your wallet connects and queries blockchain data.",
                    sections = listOf(
                        WikiSection(
                            title = "Protocol Overview",
                            paragraphs = listOf(
                                "Electrum servers speak a JSON-RPC protocol over TCP or SSL. Clients subscribe to script hashes derived from addresses, receiving notifications when transactions affect them.",
                                "Servers index the blockchain to answer queries quickly. They can operate as public infrastructure or be run privately on top of a full node."
                            )
                        ),
                        WikiSection(
                            title = "Selecting a Server",
                            paragraphs = listOf(
                                "Public servers are convenient but collect usage metadata. Running your own (e.g., Electrs, Fulcrum) keeps queries local and lets you validate responses.",
                                "UtxoPocket allows custom server configuration and surfaces certificate fingerprints so you can verify you are connecting to the right host—even over Tor."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "bitcoin-dev-kit",
                    title = "Bitcoin Dev Kit",
                    summary = "BDK is the engine powering sync and queries.",
                    sections = listOf(
                        WikiSection(
                            title = "What BDK Provides",
                            paragraphs = listOf(
                                "Bitcoin Dev Kit is a Rust library with bindings for Kotlin. It manages descriptors, address derivation, blockchain backends, and PSBT workflows.",
                                "BDK abstracts over multiple backends (Electrum, Esplora, RPC) and handles local caching of transactions and UTXO sets, so the app stays responsive."
                            )
                        ),
                        WikiSection(
                            title = "Why UtxoPocket Uses BDK",
                            paragraphs = listOf(
                                "BDK fits the MVVM + Clean architecture: the domain layer delegates wallet logic to BDK, while presentation stays focused on UI state. It keeps watch-only logic consistent across Android platforms.",
                                "Because BDK is modular, adding new backends or policy engines (like Miniscript) becomes a configuration change rather than a rewrite."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "transactions-control",
            title = "🟤 Transactions & Control",
            description = "From transaction creation to signing and advanced UTXO management.",
            topics = listOf(
                WikiTopic(
                    id = "psbt-explained",
                    title = "PSBT Explained",
                    summary = "Partially Signed Bitcoin Transactions made simple.",
                    sections = listOf(
                        WikiSection(
                            title = "Why PSBT Exists",
                            paragraphs = listOf(
                                "PSBT (BIP174) standardizes how wallets exchange partially complete transactions. It carries all metadata needed for signing—inputs, scripts, derivation paths, and more.",
                                "This format enables multi-device workflows: a watch-only wallet can draft a PSBT, send it to an offline signer, and later finalize it for broadcast without ambiguity."
                            )
                        ),
                        WikiSection(
                            title = "Lifecycle of a PSBT",
                            paragraphs = listOf(
                                "Draft: construct inputs, outputs, and include key path information. Sign: each participant adds signatures and updates the PSBT. Finalize: combine signatures, produce a fully signed transaction, and broadcast.",
                                "Because PSBTs are additive, you can involve hardware wallets, multisig cosigners, or automated policy engines without losing data. This topic is included for general knowledge; PSBT drafting is not currently available in UtxoPocket."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "transaction-signing",
                    title = "Transaction Signing",
                    summary = "How and where signatures happen in BDK.",
                    sections = listOf(
                        WikiSection(
                            title = "Signing Pipeline",
                            paragraphs = listOf(
                                "BDK assembles transaction digests based on the script type (legacy, SegWit, Taproot) and hands them to signing devices or software. Each input can require one or multiple signatures.",
                                "In watch-only mode, private keys never touch the device. Some tools export PSBTs with the metadata needed by a signing wallet or hardware device; UtxoPocket does not currently export PSBTs."
                            )
                        ),
                        WikiSection(
                            title = "Security Considerations",
                            paragraphs = listOf(
                                "Always verify change outputs and fees on the signing device. A compromised host could swap addresses or bump fees. PSBT fields like sighash type and derivation paths help detect tampering.",
                                "After signing, ensure the final transaction is broadcast over a trusted path—ideally the same Tor-enabled backend used for syncing."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "coin-control",
                    title = "Coin Control",
                    summary = "Selecting specific UTXOs for better privacy.",
                    sections = listOf(
                        WikiSection(
                            title = "Manual Selection",
                            paragraphs = listOf(
                                "Coin control lets you choose which UTXOs fund a transaction, preventing accidental linkage between unrelated funds. You can avoid spending doxxed coins with private ones.",
                                "It also helps manage UTXO count: sweeping small outputs into a single one reduces future fees, while leaving them untouched can preserve privacy if you need separate pockets."
                            )
                        ),
                        WikiSection(
                            title = "Practical Strategies",
                            paragraphs = listOf(
                                "Group UTXOs by source (income, personal savings, collaborative transactions) and pick accordingly. Keep track of change outputs—they re-enter your wallet as new UTXOs.",
                                "Use labeling to remember why a coin exists. In multi-sig setups, coordinate with cosigners so everyone understands which coins are safe to merge."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "labeling-metadata",
                    title = "Labeling & Metadata",
                    summary = "How to keep track of your transactions meaningfully.",
                    sections = listOf(
                        WikiSection(
                            title = "Why Metadata Matters",
                            paragraphs = listOf(
                                "As your UTXO set grows, human context fades. Labels help you recall who paid you, what a transaction was for, and whether an output is earmarked for taxes or collaborative custody.",
                                "Metadata also speeds up audits, makes compliance easier, and supports inheritance planning. Without it, reconstructing history can become guesswork."
                            )
                        ),
                        WikiSection(
                            title = "Maintaining Clean Records",
                            paragraphs = listOf(
                                "Use consistent naming conventions, tag transactions by project, and note when UTXOs become tainted or linked to identifiers (like invoices).",
                                "Store metadata securely—preferably encrypted backups that travel with your descriptors. Losing labels can be nearly as painful as losing access altogether."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "protocol-blocks",
            title = "🧱 Protocol, Blocks & TX",
            description = "How blocks form, how transactions get validated, and why PoW and difficulty matter.",
            topics = listOf(
                WikiTopic(
                    id = "block-and-pow",
                    title = "Blocks, PoW, and Difficulty",
                    summary = "From hash to block: the foundation of Bitcoin security.",
                    sections = listOf(
                        WikiSection(
                            title = "Block Structure",
                            paragraphs = listOf(
                                "Each block includes a header (version, prev_block, merkle_root, time, nBits, nonce) and a set of transactions. The SegWit consensus rule defines block weight in weight units (WU), which determines how much space is available.",
                                "Blocks propagate through the P2P network and short-lived reorgs can occur. Understanding this helps you interpret confirmations and temporal risk."
                            )
                        ),
                        WikiSection(
                            title = "Proof-of-Work and Difficulty Adjustment",
                            paragraphs = listOf(
                                "The network retargets difficulty every 2016 blocks to keep ~10-minute spacing. Each halving cuts issuance in half after a set number of blocks.",
                                "For users, this explains why confirmation times and fees fluctuate and why block space is scarce."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "tx-anatomy",
                    title = "Transaction Anatomy",
                    summary = "Inputs/outputs, scripts, and witnesses.",
                    sections = listOf(
                        WikiSection(
                            title = "Transaction Components",
                            paragraphs = listOf(
                                "A transaction carries a version, a list of inputs (references to previous UTXOs with unlocking data), a list of outputs (payment script and value), a locktime, and—if SegWit or Taproot—one witness per input.",
                                "A watch-only wallet does not sign; typically it prepares a PSBT with metadata so the signer can verify scripts, change outputs, and fees. UtxoPocket does not currently provide PSBT preparation."
                            )
                        ),
                        WikiSection(
                            title = "Useful Timelocks",
                            paragraphs = listOf(
                                "nLockTime (absolute) and CSV/CLTV (relative or absolute) let you defer or condition spends. Combined with Miniscript they enable vaults and recovery itineraries."
                            )
                        )
                    )
                )
            )
        ),

        WikiCategory(
            id = "hd-and-bips",
            title = "🧭 HD Derivation & BIPs",
            description = "Standardized paths for interoperability, from BIP32/39 through BIP84/86 and multisig.",
            topics = listOf(
                WikiTopic(
                    id = "hd-derivation",
                    title = "Derivation Paths",
                    summary = "From the seed to xpub/xprv and accounts.",
                    sections = listOf(
                        WikiSection(
                            title = "From BIP39 to BIP32",
                            paragraphs = listOf(
                                "A mnemonic (12-24 words) generates a seed from which hierarchical (HD) keys are derived. Wallets agree on paths (purpose/coin/account/change/index) to stay interoperable.",
                                "Examples: m/49'/0'/0' (P2SH-P2WPKH), m/84'/0'/0' (P2WPKH), m/86'/0'/0' (P2TR). For multisig, m/48'/0'/0'/2' (P2WSH)."
                            )
                        ),
                        WikiSection(
                            title = "Good Practices",
                            paragraphs = listOf(
                                "Document the fingerprint and key origins when sharing descriptors. Avoid mixing accounts or formats in one policy unless everything is clearly labeled."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "address-and-uri-standards",
                    title = "Addresses & URIs",
                    summary = "bech32/bech32m, URIs, and interoperable QR codes.",
                    sections = listOf(
                        WikiSection(
                            title = "Address Formats",
                            paragraphs = listOf(
                                "bech32 (v0: P2WPKH/P2WSH) and bech32m (v1+: Taproot) improve error detection and reduce weight. Nested-SegWit exists for compatibility.",
                                "Show users how Taproot can improve script-path privacy and how descriptors express it with tr()."
                            )
                        ),
                        WikiSection(
                            title = "URIs and QR Codes",
                            paragraphs = listOf(
                                "BIP21 defines bitcoin links with amount and label parameters. For PSBT, consider UR or BBQr (animated QR codes) for air-gapped flows."
                            )
                        )
                    )
                )
            )
        ),

        WikiCategory(
            id = "fees-advanced",
            title = "💸 Advanced Fees & Coin Selection",
            description = "RBF/CPFP, safe consolidations, and selection algorithms.",
            topics = listOf(
                WikiTopic(
                    id = "rbf-cpfp",
                    title = "RBF & CPFP",
                    summary = "How to accelerate or rescue confirmations.",
                    sections = listOf(
                        WikiSection(
                            title = "When to Use RBF/CPFP",
                            paragraphs = listOf(
                                "RBF lets you replace an unconfirmed transaction with a higher-fee version if it was marked replaceable. CPFP creates a high-fee child transaction to pull its parent through.",
                                "Surface in the UI whether a transaction is RBF opt-in and detect packages eligible for CPFP."
                            )
                        ),
                        WikiSection(
                            title = "Risks and UX",
                            paragraphs = listOf(
                                "Avoid using RBF when a recipient does not accept it. For CPFP, clarify that the child must spend one of the parent's outputs (for example, the change)."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "coin-selection-algos",
                    title = "Selection Algorithms",
                    summary = "BnB, Knapsack, and Single Random Draw.",
                    sections = listOf(
                        WikiSection(
                            title = "Goal: Minimize Waste",
                            paragraphs = listOf(
                                "Bitcoin Core tries several strategies and optimizes for \"waste\". Sometimes spending without change (exact match) is best; other times you minimize change size and avoid mixing origins."
                            )
                        ),
                        WikiSection(
                            title = "Privacy vs Cost",
                            paragraphs = listOf(
                                "Do not merge UTXOs with different histories if it weakens your threat model. Consolidate during mempool lulls, but use fresh change paths and clear labels."
                            )
                        )
                    )
                )
            )
        ),

        WikiCategory(
            id = "scripts-descriptors-policy",
            title = "Scripts, Descriptors & Policy",
            description = "Structures and languages that define how and when your coins can be spent.",
            topics = listOf(
                WikiTopic(
                    id = "descriptors-101",
                    title = "Descriptors 101",
                    summary = "Understand how watch-only descriptors work.",
                    sections = listOf(
                        WikiSection(
                            title = "What Descriptors Express",
                            paragraphs = listOf(
                                "Descriptors describe address derivation in plain text: script type + key origin paths + fingerprints. For example, \"wpkh([d34db33f/84h/0h/0h]xpub.../0/*)\" defines a native SegWit account.",
                                "They act as portable, unambiguous blueprints that different wallets can interpret consistently, reducing reliance on proprietary metadata."
                            )
                        ),
                        WikiSection(
                            title = "Benefits for Watch-Only",
                            paragraphs = listOf(
                                "With descriptors, a watch-only wallet can derive receive addresses, detect change paths, and verify multi-sig scripts without private keys.",
                                "Descriptors also simplify backup strategy: store them alongside your seed to recreate the same wallet structure anywhere, avoiding mismatch between software implementations."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "miniscript",
                    title = "Miniscript",
                    summary = "A safer language for Bitcoin spending conditions.",
                    sections = listOf(
                        WikiSection(
                            title = "Overview",
                            paragraphs = listOf(
                                "Miniscript is a structured representation of Bitcoin script focused on safety and composability. It limits scripts to a subset that is analyzable and policy-friendly.",
                                "It supports familiar conditions like multi-signature thresholds, timelocks, and hash preimages, but encodes them in a tree format that tools can reason about."
                            )
                        ),
                        WikiSection(
                            title = "Why It Matters",
                            paragraphs = listOf(
                                "By expressing policies in Miniscript, wallets can automatically derive spending conditions, estimate fees, and ensure there are no hidden denial-of-service vectors.",
                                "Miniscript pairs naturally with descriptors (tr() syntax) and enables more complex vaulting schemes without resorting to custom scripts."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "spending-policies",
                    title = "Spending Policies",
                    summary = "Define flexible spending rules using Miniscript.",
                    sections = listOf(
                        WikiSection(
                            title = "Composing Rules",
                            paragraphs = listOf(
                                "Policies combine clauses like \"2 of these 3 keys\" OR \"after 30 days a single recovery key\". Miniscript ensures the resulting script is valid and efficient.",
                                "This unlocks advanced setups such as time-locked vaults, inheritance paths, or business workflows requiring approval from specific roles."
                            )
                        ),
                        WikiSection(
                            title = "Operational Lifecycle",
                            paragraphs = listOf(
                                "Define policies off-chain, translate them to descriptors, and distribute them to cosigners. Keep documentation synchronized so everyone knows the rules.",
                                "Monitor for policy drift: if a key is rotated or a signer leaves, update descriptors and communicate changes promptly. Automation reduces room for human error."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "security-ops",
            title = "Security & Ops",
            description = "Best practices for securing your wallet and operating environment.",
            topics = listOf(
                WikiTopic(
                    id = "operational-security",
                    title = "Operational Security",
                    summary = "Best practices for securing your wallet and device.",
                    sections = listOf(
                        WikiSection(
                            title = "Device Hygiene",
                            paragraphs = listOf(
                                "Keep your operating system patched, use full-disk encryption, and avoid installing untrusted apps. Malware is the fastest path to losing funds.",
                                "Isolate critical tasks on dedicated hardware when possible. Air-gapped signers and watch-only monitors reduce the blast radius of compromise."
                            )
                        ),
                        WikiSection(
                            title = "Wallet-Specific Practices",
                            paragraphs = listOf(
                                "Protect your Tor endpoint and backend credentials. Monitor logs for suspicious access and rotate secrets periodically.",
                                "Train on incident response: know how to revoke compromised descriptors, rotate keys, and restore from clean backups without hesitation."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "environments-future",
            title = "Environments & Future",
            description = "Testing environments and technologies shaping Bitcoin's evolution.",
            topics = listOf(
                WikiTopic(
                    id = "testnet-regtest",
                    title = "Testnet & Regtest",
                    summary = "Safe environments for development and testing.",
                    sections = listOf(
                        WikiSection(
                            title = "Comparing Test Networks",
                            paragraphs = listOf(
                                "Testnet is a public network with free coins and real-world latency. Blocks are mined irregularly, which makes it good for staging realistic user flows without risking mainnet funds.",
                                "Regtest is a private chain you control locally. You can mine blocks on demand, simulate reorgs, and script scenarios—perfect for automated tests and reproducible demos."
                            )
                        ),
                        WikiSection(
                            title = "Workflow Integration",
                            paragraphs = listOf(
                                "Use descriptors that match your production setup so migrations are smooth. Testing watch-only flows on regtest catches derivation or policy issues early.",
                                "Automate funding, transaction generation, and syncing in CI pipelines so QA can replay edge cases without waiting on public miners."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "testnet-faucets",
                    title = "Testnet Faucets",
                    summary = "Reliable sources of testnet bitcoin for experimentation.",
                    sections = listOf(
                        WikiSection(
                            title = "Why Faucets Matter",
                            paragraphs = listOf(
                                "Public test networks reset or slow down from time to time, so keeping a small stash of coins handy helps you reproduce customer issues or validate complex flows.",
                                "Most faucets enforce rate limits, captchas, or social sign-ins. If one service is dry, cycle through alternatives and plan for occasional delays."
                            )
                        ),
                        WikiSection(
                            title = "Testnet3 Faucets",
                            paragraphs = listOf(
                                "bitcoinfaucet.uo1.net dispenses a predictable amount after solving a captcha. It is suited for quick wallet funding during demos.",
                                "coinfaucet.eu/en/btc-testnet/ offers larger payouts but requires a short waiting period between requests."
                            )
                        ),
                        WikiSection(
                            title = "Testnet4 Faucets",
                            paragraphs = listOf(
                                "faucet.testnet4.dev is maintained by the Bitcoin Core team and supports the newest chain parameters introduced with testnet4.",
                                "coinfaucet.eu/en/btc-testnet4/ mirrors its testnet3 counterpart for the upgraded network.",
                                "mempool.space/testnet4/faucet integrates directly with the popular explorer so you can confirm transactions without switching tabs."
                            )
                        )
                    )
                ),
                WikiTopic(
                    id = "bitcoin-future-tech",
                    title = "Bitcoin's Future Tech",
                    summary = "Taproot, Lightning, and other protocol evolutions.",
                    sections = listOf(
                        WikiSection(
                            title = "Taproot and Beyond",
                            paragraphs = listOf(
                                "Taproot (BIP341/342) introduced key-path spending and script-path privacy, laying the groundwork for more expressive contracts with smaller on-chain footprints.",
                                "Ongoing work on covenants, ANYPREVOUT, and OP_CAT revival explores new ways to enforce spending conditions, vaults, and congestion control."
                            )
                        ),
                        WikiSection(
                            title = "Second-Layer Momentum",
                            paragraphs = listOf(
                                "Lightning Network brings instant, low-fee payments via bidirectional channels. Watch-only tools need to monitor channel states and HTLC resolutions.",
                                "Other emerging tech—like Fedimint, Ark, or sidechains—aims to balance scalability with user sovereignty. Staying informed helps you assess when to integrate new capabilities."
                            )
                        )
                    )
                )
            )
        ),
        WikiCategory(
            id = "utxopocket",
            title = "UtxoPocket",
            description = "How the application is assembled, operated, and extended.",
            topics = listOf(
                WikiTopic(
                    id = "utxopocket-overview",
                    title = "UtxoPocket Overview",
                    summary = "Understand the product pillars, automation, and shared descriptor flow that power UtxoPocket.",
                    sections = listOf(
                        WikiSection(
                            title = "Product Pillars",
                            paragraphs = listOf(
                                "UtxoPocket is a privacy-first, watch-only wallet tailored for operators who monitor multiple descriptors across networks. It routes every network call through Tor, encrypts local data at rest, and keeps the UI responsive even while long-running sync jobs execute in the background.",
                                "Descriptors are the source of truth: the app never holds signing keys. Instead, it verifies balances, tracks address pools, and surfaces health heuristics so teams can coordinate spending decisions from a dedicated signer or hardware device."
                            )
                        ),
                        WikiSection(
                            title = "Shared Descriptor Mode",
                            paragraphs = listOf(
                                "Enable shared descriptors when the same descriptor pair is observed by other wallets or services. UtxoPocket widens the discovery gap limit, queues a full rescan, and refreshes address pools so deposits revealed elsewhere show up promptly.",
                                "Disabling the toggle narrows the gap limit and skips wide rescans, ideal when UtxoPocket is the single watcher. Toggling the setting always prompts a rescan and the UI blocks further changes while the repository updates SQLite and schedules the job."
                            )
                        ),
                        WikiSection(
                            title = "Supported Descriptor Formats",
                            paragraphs = listOf(
                                "UtxoPocket accepts watch-only descriptors that follow BIP-380 and expose public derivation data with wildcard paths (`*`) or a BIP-389 multipath tuple (`/<0;1>/*`). Provide both external and change branches—either as a descriptor pair or as a single multipath definition—for script families such as `wpkh(...)`, `sh(wpkh(...))`, `tr(...)`, and `wsh(sortedmulti(...))`. Miniscript policies are supported as long as they satisfy the same derivation rules.",
                                "Descriptors lacking wildcard derivation (for example `wpkh(pubkey)`) are rejected because BDK requires distinct external and internal branches. To track a fixed script, wrap it with `addr(...)`, which the app marks as a view-only wallet. Descriptors containing private key material (`xprv`, WIF, miniscript secrets) are never accepted."
                            )
                        ),
                        WikiSection(
                            title = "Health Analytics",
                            paragraphs = listOf(
                                "Transaction Health evaluates each history entry for address reuse, batching patterns, script diversity, and fee alignment. Results surface as contextual badges so you can spot risky flows without leaving the timeline.",
                                "UTXO Health highlights consolidation gaps, lingering change outputs, and dust posture, while Wallet Health aggregates the signals into privacy, efficiency, and risk pillars. You can toggle these modules from Settings → Privacy & analysis."
                            )
                        ),
                        WikiSection(
                            title = "Operations in Practice",
                            paragraphs = listOf(
                                "Every network request routes through the embedded Tor client. The global status bar shows bootstrap progress and node health, and you can renew the Tor identity from Settings whenever you need a clean circuit.",
                                "Balance cards warn about reused addresses, show scheduled full scans, and expose one-tap tooling to copy descriptors, display QR codes, or trigger a deep rescan. Watch-only operators get full visibility without risking signing keys."
                            )
                        )
                    )
                )
            )
        )
    ).map { category ->
        val topicsWithKeywords = category.topics.map { topic ->
            if (topic.keywords.isNotEmpty()) {
                topic
            } else {
                topic.copy(keywords = wikiTopicKeywords[topic.id].orEmpty())
            }
        }
        category.copy(topics = topicsWithKeywords)
    }

    val topicsById: Map<String, WikiTopic> = categories
        .flatMap { it.topics }
        .associateBy { it.id }
}
