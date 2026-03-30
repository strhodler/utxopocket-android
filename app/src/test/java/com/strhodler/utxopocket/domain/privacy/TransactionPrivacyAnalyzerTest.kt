package com.strhodler.utxopocket.domain.privacy

import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionPrivacyAnalyzerTest {

    private val analyzer = TransactionPrivacyAnalyzer(
        crossHeuristicRules = CrossHeuristicRules()
    )

    @Test
    fun analyzeMarksMultiInputOwnershipWithMediumConfidenceForOwnedFanIn() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(prevTxid = "in-1", isMine = true),
                    input(prevTxid = "in-2", isMine = true)
                ),
                outputs = listOf(
                    output(index = 0, valueSats = 150_000, isMine = false)
                )
            )
        )

        assertEquals(
            PrivacyConfidence.Medium,
            findings.first { it.id == PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP }.confidence
        )
    }

    @Test
    fun analyzeDowngradesMultiInputOwnershipConfidenceWhenInputsAreMixed() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(prevTxid = "in-1", isMine = true),
                    input(prevTxid = "in-2", isMine = true),
                    input(prevTxid = "in-3", isMine = false)
                ),
                outputs = listOf(
                    output(index = 0, valueSats = 220_000, isMine = false)
                )
            )
        )

        assertEquals(
            PrivacyConfidence.Low,
            findings.first { it.id == PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP }.confidence
        )
    }

    @Test
    fun analyzeFlagsConsolidationFanInForManyOwnedInputs() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(prevTxid = "in-1", isMine = true),
                    input(prevTxid = "in-2", isMine = true),
                    input(prevTxid = "in-3", isMine = true),
                    input(prevTxid = "in-4", isMine = true)
                ),
                outputs = listOf(
                    output(index = 0, valueSats = 420_000, isMine = true, addressType = WalletAddressType.CHANGE)
                )
            )
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.TRANSACTION_CONSOLIDATION_FAN_IN })
    }

    @Test
    fun analyzeFlagsSelfTransferAndSuppressesRedundantChangeFinding() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(prevTxid = "in-1", isMine = true),
                    input(prevTxid = "in-2", isMine = true)
                ),
                outputs = listOf(
                    output(index = 0, valueSats = 60_000, isMine = true),
                    output(index = 1, valueSats = 140_000, isMine = true, addressType = WalletAddressType.CHANGE)
                )
            )
        )

        val findingIds = findings.map(PrivacyFinding::id)
        assertTrue(findingIds.contains(PrivacyFindingIds.TRANSACTION_SELF_TRANSFER))
        assertFalse(findingIds.contains(PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE))
    }

    @Test
    fun analyzeFlagsProbableChangeWhenOutputsShowExternalAndOwnedSplit() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(prevTxid = "in-1", isMine = true)
                ),
                outputs = listOf(
                    output(index = 0, valueSats = 95_000, isMine = false, address = "bc1qexternal0000000000000000000000"),
                    output(index = 1, valueSats = 25_000, isMine = true, addressType = WalletAddressType.CHANGE)
                )
            )
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE })
    }

    @Test
    fun analyzeFlagsChangelessSpendAsPositiveSignal() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(input(prevTxid = "in-1", isMine = true)),
                outputs = listOf(
                    output(index = 0, valueSats = 85_000, isMine = false)
                )
            )
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND })
        assertFalse(findings.any { it.id == PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE })
    }

    @Test
    fun analyzeFlagsMixedAddressFamiliesAsLinkabilityHint() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(
                        prevTxid = "in-1",
                        isMine = true,
                        address = "bc1qsegwitinput0000000000000000000000000"
                    )
                ),
                outputs = listOf(
                    output(
                        index = 0,
                        valueSats = 130_000,
                        isMine = false,
                        address = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"
                    )
                )
            )
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY })
    }

    @Test
    fun analyzeKeepsCoinjoinPatternAndSuppressesOverlapPenalties() {
        val findings = analyzer.analyze(
            transaction(
                inputs = listOf(
                    input(prevTxid = "in-1", isMine = true),
                    input(prevTxid = "in-2", isMine = true),
                    input(prevTxid = "in-3", isMine = true)
                ),
                outputs = listOf(
                    output(index = 0, valueSats = 50_000, isMine = false),
                    output(index = 1, valueSats = 50_000, isMine = false),
                    output(index = 2, valueSats = 50_000, isMine = true, addressType = WalletAddressType.CHANGE)
                )
            )
        )

        val findingIds = findings.map(PrivacyFinding::id)
        assertTrue(findingIds.contains(PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN))
        assertFalse(findingIds.contains(PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP))
        assertFalse(findingIds.contains(PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE))
    }

    private fun transaction(
        id: String = "tx-privacy",
        inputs: List<WalletTransactionInput> = emptyList(),
        outputs: List<WalletTransactionOutput> = emptyList()
    ): WalletTransaction = WalletTransaction(
        id = id,
        amountSats = -120_000,
        timestamp = null,
        type = TransactionType.SENT,
        confirmations = 3,
        structure = TransactionStructure.SEGWIT,
        inputs = inputs,
        outputs = outputs
    )

    private fun input(
        prevTxid: String,
        prevVout: Int = 0,
        valueSats: Long? = 60_000,
        address: String? = "bc1qinput000000000000000000000000000000",
        isMine: Boolean,
        addressType: WalletAddressType? = if (isMine) WalletAddressType.EXTERNAL else null
    ): WalletTransactionInput = WalletTransactionInput(
        prevTxid = prevTxid,
        prevVout = prevVout,
        valueSats = valueSats,
        address = address,
        isMine = isMine,
        addressType = addressType
    )

    private fun output(
        index: Int,
        valueSats: Long,
        address: String? = "bc1qoutput0000000000000000000000000000",
        isMine: Boolean,
        addressType: WalletAddressType? = if (isMine) WalletAddressType.EXTERNAL else null
    ): WalletTransactionOutput = WalletTransactionOutput(
        index = index,
        valueSats = valueSats,
        address = address,
        isMine = isMine,
        addressType = addressType
    )
}
