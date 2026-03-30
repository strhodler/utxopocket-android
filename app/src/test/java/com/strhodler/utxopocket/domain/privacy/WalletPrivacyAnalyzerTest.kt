package com.strhodler.utxopocket.domain.privacy

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletPrivacyAnalyzerTest {

    private val analyzer = WalletPrivacyAnalyzer(
        crossHeuristicRules = CrossHeuristicRules()
    )

    @Test
    fun analyzeFlagsReusedAddressExposure() {
        val findings = analyzer.analyze(
            walletDetail(
                utxos = listOf(
                    utxo(valueSats = 120_000, address = "bc1q-reused", addressReuseCount = 3),
                    utxo(valueSats = 80_000, address = "bc1q-fresh")
                )
            )
        )

        assertTrue(findings.any { it.id == "wallet-address-reuse" })
    }

    @Test
    fun analyzeFlagsDustPressureFromDustyWalletState() {
        val findings = analyzer.analyze(
            walletDetail(
                utxos = listOf(
                    utxo(valueSats = 250),
                    utxo(valueSats = 400),
                    utxo(valueSats = 125_000)
                )
            )
        )

        assertTrue(findings.any { it.id == "wallet-dust-pressure" })
    }

    @Test
    fun analyzeFlagsFragmentationPressureForHighlyFragmentedWallet() {
        val findings = analyzer.analyze(
            walletDetail(
                utxos = List(12) { index ->
                    utxo(
                        txid = "frag-$index",
                        vout = index,
                        valueSats = 50_000,
                        address = "bc1q-frag-$index"
                    )
                }
            )
        )

        assertTrue(findings.any { it.id == "wallet-fragmentation-pressure" })
    }

    @Test
    fun analyzeFlagsLabelHygieneGapWhenMostItemsAreUnlabeled() {
        val findings = analyzer.analyze(
            walletDetail(
                transactions = listOf(
                    transaction(id = "tx-1", label = null),
                    transaction(id = "tx-2", label = null),
                    transaction(id = "tx-3", label = "salary")
                ),
                utxos = listOf(
                    utxo(txid = "tx-1", vout = 0, label = null, transactionLabel = null),
                    utxo(txid = "tx-2", vout = 0, label = null, transactionLabel = null),
                    utxo(txid = "tx-3", vout = 0, label = null, transactionLabel = "salary")
                )
            )
        )

        assertTrue(findings.any { it.id == "wallet-label-hygiene-gap" })
    }

    @Test
    fun analyzeFlagsMixedScriptFamilyExposureWhenHistorySpansMultipleStructures() {
        val findings = analyzer.analyze(
            walletDetail(
                transactions = listOf(
                    transaction(id = "legacy", structure = TransactionStructure.LEGACY),
                    transaction(id = "taproot", structure = TransactionStructure.TAPROOT)
                )
            )
        )

        assertTrue(findings.any { it.id == "wallet-mixed-script-families" })
    }

    @Test
    fun analyzeFlagsAddressFamilyMixingWhenUtxoAddressesSpanDifferentFamilies() {
        val findings = analyzer.analyze(
            walletDetail(
                utxos = listOf(
                    utxo(address = "bc1qsegwitaddress000000000000000000000000000"),
                    utxo(txid = "tx-alt", vout = 1, address = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy")
                )
            )
        )

        assertTrue(findings.any { it.id == "wallet-mixed-address-families" })
    }

    @Test
    fun analyzeFlagsToxicChangeRiskWhenSmallChangeOutputsAccumulate() {
        val findings = analyzer.analyze(
            walletDetail(
                utxos = listOf(
                    utxo(
                        txid = "chg-1",
                        vout = 0,
                        valueSats = 18_000,
                        addressType = WalletAddressType.CHANGE
                    ),
                    utxo(
                        txid = "chg-2",
                        vout = 1,
                        valueSats = 25_000,
                        addressType = WalletAddressType.CHANGE
                    ),
                    utxo(
                        txid = "chg-3",
                        vout = 2,
                        valueSats = 30_000,
                        addressType = WalletAddressType.CHANGE
                    ),
                    utxo(
                        txid = "ext-1",
                        vout = 0,
                        valueSats = 350_000,
                        addressType = WalletAddressType.EXTERNAL
                    )
                )
            )
        )

        assertTrue(findings.any { it.id == "wallet-toxic-change-risk" })
    }

    @Test
    fun analyzeReturnsPositiveHygieneFindingsForCleanWallet() {
        val findings = analyzer.analyze(
            walletDetail(
                transactions = listOf(
                    transaction(id = "tx-1", label = "salary", structure = TransactionStructure.SEGWIT),
                    transaction(id = "tx-2", label = "savings", structure = TransactionStructure.SEGWIT)
                ),
                utxos = listOf(
                    utxo(txid = "tx-1", vout = 0, valueSats = 210_000, label = "salary"),
                    utxo(txid = "tx-2", vout = 1, valueSats = 125_000, label = "savings"),
                    utxo(txid = "tx-3", vout = 0, valueSats = 90_000, transactionLabel = "reserve")
                )
            )
        )

        val ids = findings.map { it.id }.toSet()

        assertEquals(
            setOf("wallet-low-reuse", "wallet-organized-labels", "wallet-low-dust"),
            ids
        )
        assertFalse(ids.contains("wallet-address-reuse"))
        assertFalse(ids.contains("wallet-dust-pressure"))
        assertFalse(ids.contains("wallet-label-hygiene-gap"))

        assertEquals(
            PrivacyConfidence.Medium,
            findings.first { it.id == "wallet-organized-labels" }.confidence
        )
        assertEquals(
            PrivacyConfidence.Medium,
            findings.first { it.id == "wallet-low-reuse" }.confidence
        )
        assertEquals(
            PrivacyConfidence.Medium,
            findings.first { it.id == "wallet-low-dust" }.confidence
        )
    }

    @Test
    fun analyzeLabelGapEvidenceIncludesCollectionScopeLimitation() {
        val findings = analyzer.analyze(
            walletDetail(
                transactions = listOf(
                    transaction(id = "tx-1", label = null),
                    transaction(id = "tx-2", label = null),
                    transaction(id = "tx-3", label = "salary")
                ),
                utxos = listOf(
                    utxo(txid = "tx-1", vout = 0, label = null, transactionLabel = null),
                    utxo(txid = "tx-2", vout = 0, label = null, transactionLabel = null),
                    utxo(txid = "tx-3", vout = 0, label = null, transactionLabel = "salary")
                )
            )
        )

        val finding = findings.first { it.id == "wallet-label-hygiene-gap" }
        assertEquals(
            "collections-unavailable-in-wallet-detail-inputs",
            finding.evidence["scope_limitation"]
        )
    }

    @Test
    fun analyzeAppliesCrossHeuristicEscalationForSharedLinkabilityClues() {
        val findings = analyzer.analyze(
            walletDetail(
                transactions = listOf(
                    transaction(id = "legacy", structure = TransactionStructure.LEGACY),
                    transaction(id = "taproot", structure = TransactionStructure.TAPROOT)
                ),
                utxos = listOf(
                    utxo(valueSats = 120_000, address = "bc1q-reused", addressReuseCount = 2)
                )
            )
        )

        assertEquals(
            PrivacySeverity.Critical,
            findings.first { it.id == "wallet-address-reuse" }.severity
        )
        assertEquals(
            PrivacySeverity.Warning,
            findings.first { it.id == "wallet-mixed-script-families" }.severity
        )
    }

    private fun walletDetail(
        transactions: List<WalletTransaction> = emptyList(),
        utxos: List<WalletUtxo> = emptyList()
    ): WalletDetail = WalletDetail(
        summary = WalletSummary(
            id = 1L,
            name = "Privacy Wallet",
            balanceSats = utxos.sumOf { it.valueSats },
            transactionCount = transactions.size,
            utxoCount = utxos.size,
            network = BitcoinNetwork.TESTNET4,
            lastSyncStatus = NodeStatus.Synced,
            lastSyncTime = null,
            descriptorType = DescriptorType.P2WPKH
        ),
        descriptor = "wpkh([abcd1234/84'/1'/0']tpubD6NzVbkrYhZ4Yplaceholder/0/*)",
        transactions = transactions,
        utxos = utxos
    )

    private fun transaction(
        id: String,
        label: String? = null,
        structure: TransactionStructure = TransactionStructure.SEGWIT
    ): WalletTransaction = WalletTransaction(
        id = id,
        amountSats = 50_000,
        timestamp = null,
        type = TransactionType.RECEIVED,
        confirmations = 6,
        label = label,
        structure = structure
    )

    private fun utxo(
        txid: String = "txid",
        vout: Int = 0,
        valueSats: Long = 50_000,
        label: String? = null,
        transactionLabel: String? = null,
        address: String? = "bc1q-test",
        addressReuseCount: Int = 1,
        addressType: WalletAddressType? = null,
        derivationPath: String? = null
    ): WalletUtxo = WalletUtxo(
        txid = txid,
        vout = vout,
        valueSats = valueSats,
        confirmations = 6,
        label = label,
        transactionLabel = transactionLabel,
        address = address,
        addressReuseCount = addressReuseCount,
        addressType = addressType,
        derivationPath = derivationPath
    )
}
