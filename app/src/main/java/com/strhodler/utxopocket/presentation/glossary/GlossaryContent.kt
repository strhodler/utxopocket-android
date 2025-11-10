package com.strhodler.utxopocket.presentation.glossary

data class GlossaryEntry(
    val id: String,
    val term: String,
    val shortDescription: String,
    val definition: List<String>,
    val aliases: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

object GlossaryContent {
    val entries: List<GlossaryEntry> = listOf(
        GlossaryEntry(
            id = "utxo",
            term = "UTXO",
            shortDescription = "Unspent transaction output that holds spendable bitcoin.",
            definition = listOf(
                "A UTXO (Unspent Transaction Output) is a discrete chunk of bitcoin recorded on the blockchain. When you spend funds, specific UTXOs become inputs to your transaction and are fully consumed. Any leftover value is sent back to you as a brand-new output, commonly called change.",
                "Wallet balances are the sum of every UTXO you control. Managing UTXOs well lets you optimize fees, preserve privacy, and avoid linking unrelated funds when spending."
            ),
            aliases = listOf("unspent output", "coin"),
            keywords = listOf(
                "Coin control",
                "Inputs",
                "Fee efficiency",
                "Privacy",
                "Coin set",
                "Dust",
                "Chain analysis",
                "Ownership"
            )
        ),
        GlossaryEntry(
            id = "descriptor",
            term = "Descriptor",
            shortDescription = "A human-readable template that defines how wallet addresses are derived.",
            definition = listOf(
                "Output descriptors are strings that describe the exact script template and derivation path used to generate addresses. They encode script type (such as native SegWit), key origins, and derivation wildcards.",
                "Descriptors make it simple to reproduce a watch-only wallet on multiple devices without sharing private keys. UtxoPocket relies on descriptors to monitor balances securely."
            ),
            aliases = listOf("output descriptor"),
            keywords = listOf(
                "Watch-only",
                "Derivation paths",
                "Multi-sig",
                "Script policy",
                "Wallet policy",
                "Multisig template",
                "Descriptor checksum"
            )
        ),
        GlossaryEntry(
            id = "seed-phrase",
            term = "Seed phrase",
            shortDescription = "Mnemonic words that back up the keys controlling your bitcoin.",
            definition = listOf(
                "A seed phrase (BIP39 mnemonic) is a list of 12 to 24 words that encodes the entropy used to derive all of your wallet keys. As long as you retain the words—and any optional passphrase—you can recover the wallet on compatible software.",
                "Store the seed phrase offline, protect it from fire and water, and practice restoring it in a safe environment to ensure there are no transcription errors."
            ),
            aliases = listOf("mnemonic", "recovery phrase"),
            keywords = listOf(
                "Backup",
                "Passphrase",
                "BIP39",
                "Disaster recovery",
                "Entropy",
                "Seed backup",
                "Seed passphrase",
                "Metal backup",
                "Recovery test"
            )
        ),
        GlossaryEntry(
            id = "electrum-server",
            term = "Electrum server",
            shortDescription = "Server that provides wallet data using the Electrum protocol.",
            definition = listOf(
                "An Electrum server maintains Bitcoin blockchain data and indexes addresses so light clients can query balances and history. Wallets connect to a server over TCP/TLS to broadcast transactions and fetch updates.",
                "Running your own server—or using one you trust—prevents third parties from learning which addresses belong to you. UtxoPocket can connect to public or self-hosted Electrum servers."
            ),
            aliases = listOf("electrumx", "esplora server"),
            keywords = listOf(
                "Self-hosting",
                "Connectivity",
                "Privacy",
                "Network backend",
                "ElectrumX",
                "Electrs",
                "Backend node",
                "Light client",
                "Server privacy"
            )
        ),
        GlossaryEntry(
            id = "tor",
            term = "Tor",
            shortDescription = "Privacy network that routes traffic through relays to hide your IP.",
            definition = listOf(
                "Tor (The Onion Router) is a network that anonymizes your internet traffic by passing it through multiple encrypted hops. Each relay knows only its immediate neighbors, keeping the original IP address hidden.",
                "UtxoPocket routes Electrum connections through Tor by default, reducing the risk of linking your wallet activity to your real-world identity."
            ),
            aliases = listOf("onion routing"),
            keywords = listOf(
                "Privacy",
                "Network security",
                "Relay",
                "Traffic analysis",
                "Onion service",
                "SOCKS5",
                "Bridge",
                "Circuit",
                "Exit node"
            )
        ),
        GlossaryEntry(
            id = "coin-control",
            term = "Coin control",
            shortDescription = "Manual selection of UTXOs to decide which coins fund a transaction.",
            definition = listOf(
                "Coin control tools let you choose exactly which UTXOs become inputs when building a transaction. This is useful for minimizing fees, keeping dust from growing, and protecting privacy by avoiding unnecessary linkage between coins.",
                "Advanced users apply coin control to separate long-term savings from everyday spending and to craft collaborative transactions such as CoinJoin."
            ),
            aliases = listOf("input selection"),
            keywords = listOf(
                "Fee management",
                "Privacy",
                "Spend strategy",
                "UTXO selection",
                "Manual coin pick",
                "Privacy batching",
                "Dust cleanup"
            )
        )
    )
}
