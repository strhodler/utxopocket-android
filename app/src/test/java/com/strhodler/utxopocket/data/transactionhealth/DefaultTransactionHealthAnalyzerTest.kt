package com.strhodler.utxopocket.data.transactionhealth

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicatorType
import com.strhodler.utxopocket.domain.model.TransactionHealthContext
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTransactionHealthAnalyzerTest {

    private val analyzer = DefaultTransactionHealthAnalyzer()

    @Test
    fun analyze_marksAddressReuseOnlyAfterFirstAppearance() {
        val tx1 = createSentTransaction(
            id = "tx1",
            timestamp = 10L,
            changeAddress = "bc1-change",
            changeValue = 12_000L,
            changeType = WalletAddressType.CHANGE,
            externalValue = 50_000L,
            feeRate = 5.0
        )
        val tx2 = createSentTransaction(
            id = "tx2",
            timestamp = 20L,
            changeAddress = "bc1-change",
            changeValue = 11_000L,
            changeType = WalletAddressType.CHANGE,
            externalValue = 52_000L,
            feeRate = 4.0
        )

        val detail = walletDetail(listOf(tx1, tx2))
        val summary = analyzer.analyze(
            detail,
            dustThresholdSats = 546L,
            parameters = TransactionHealthParameters()
        )

        val firstIndicators = summary.transactions.getValue("tx1").indicators
        assertFalse(firstIndicators.any { it.type == TransactionHealthIndicatorType.ADDRESS_REUSE })

        val secondIndicators = summary.transactions.getValue("tx2").indicators
        assertTrue(secondIndicators.any { it.type == TransactionHealthIndicatorType.ADDRESS_REUSE })
    }

    @Test
    fun analyzeTransaction_detectsChangeExposureAndLowFeePosture() {
        val transaction = createSentTransaction(
            id = "tx-change",
            timestamp = 30L,
            changeAddress = "bc1-change-2",
            changeValue = 1_000L,
            changeType = WalletAddressType.CHANGE,
            externalValue = 50_000L,
            feeRate = 0.5
        )

        val result = analyzer.analyzeTransaction(
            transaction,
            TransactionHealthContext(
                emptySet(),
                dustThresholdSats = 500L,
                parameters = TransactionHealthParameters()
            )
        )

        val changeIndicator = result.indicators.firstOrNull {
            it.type == TransactionHealthIndicatorType.CHANGE_EXPOSURE
        }
        assertTrue(changeIndicator != null && changeIndicator.delta <= -6)

        assertTrue(
            result.indicators.any { it.type == TransactionHealthIndicatorType.FEE_UNDERPAY }
        )
        assertTrue(result.finalScore < 100)
    }

    @Test
    fun analyzeTransaction_detectsDustIncomingAndOutgoing() {
        val incoming = WalletTransaction(
            id = "tx-recv",
            amountSats = 200L,
            timestamp = 40L,
            type = TransactionType.RECEIVED,
            confirmations = 1,
            feeRateSatPerVb = null,
            inputs = emptyList(),
            outputs = listOf(
                WalletTransactionOutput(
                    index = 0,
                    valueSats = 200L,
                    address = "bc1-dust-in",
                    isMine = true,
                    addressType = WalletAddressType.EXTERNAL
                )
            )
        )

        val incomingResult = analyzer.analyzeTransaction(
            incoming,
            TransactionHealthContext(
                emptySet(),
                dustThresholdSats = 300L,
                parameters = TransactionHealthParameters()
            )
        )
        assertTrue(
            incomingResult.indicators.any { it.type == TransactionHealthIndicatorType.DUST_INCOMING }
        )

        val outgoing = WalletTransaction(
            id = "tx-send",
            amountSats = -3_000L,
            timestamp = 50L,
            type = TransactionType.SENT,
            confirmations = 1,
            feeRateSatPerVb = 5.0,
            inputs = listOf(
                WalletTransactionInput(
                    prevTxid = "prev",
                    prevVout = 0,
                    valueSats = 200L,
                    address = "bc1-input-dust",
                    isMine = true
                )
            ),
            outputs = listOf(
                WalletTransactionOutput(
                    index = 0,
                    valueSats = 1_500L,
                    address = "bc1-external",
                    isMine = false
                ),
                WalletTransactionOutput(
                    index = 1,
                    valueSats = 1_500L,
                    address = "bc1-change-fair",
                    isMine = true,
                    addressType = WalletAddressType.CHANGE
                )
            )
        )

        val outgoingResult = analyzer.analyzeTransaction(
            outgoing,
            TransactionHealthContext(
                emptySet(),
                dustThresholdSats = 300L,
                parameters = TransactionHealthParameters()
            )
        )

        assertTrue(
            outgoingResult.indicators.any { it.type == TransactionHealthIndicatorType.DUST_OUTGOING }
        )
        assertEquals(
            2,
            outgoingResult.badges.size
        )
    }

    private fun walletDetail(transactions: List<WalletTransaction>): WalletDetail {
        val summary = WalletSummary(
            id = 1L,
            name = "Test Wallet",
            balanceSats = 0L,
            transactionCount = transactions.size,
            network = BitcoinNetwork.MAINNET,
            lastSyncStatus = NodeStatus.Synced,
            lastSyncTime = 0L,
            color = WalletColor.DEFAULT
        )
        return WalletDetail(
            summary = summary,
            descriptor = "wpkh(... )",
            transactions = transactions
        )
    }

    private fun createSentTransaction(
        id: String,
        timestamp: Long,
        changeAddress: String,
        changeValue: Long,
        changeType: WalletAddressType,
        externalValue: Long,
        feeRate: Double
    ): WalletTransaction {
        return WalletTransaction(
            id = id,
            amountSats = -externalValue - changeValue,
            timestamp = timestamp,
            type = TransactionType.SENT,
            confirmations = 2,
            feeRateSatPerVb = feeRate,
            inputs = listOf(
                WalletTransactionInput(
                    prevTxid = "${id}_input",
                    prevVout = 0,
                    valueSats = externalValue + changeValue + 1_000L,
                    address = "bc1-input-$id",
                    isMine = true
                )
            ),
            outputs = listOf(
                WalletTransactionOutput(
                    index = 0,
                    valueSats = externalValue,
                    address = "bc1-external-$id",
                    isMine = false
                ),
                WalletTransactionOutput(
                    index = 1,
                    valueSats = changeValue,
                    address = changeAddress,
                    isMine = true,
                    addressType = changeType
                )
            )
        )
    }
}
