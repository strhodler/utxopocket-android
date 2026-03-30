package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.privacy.PrivacyConfidence
import com.strhodler.utxopocket.domain.privacy.PrivacyFinding
import com.strhodler.utxopocket.domain.privacy.PrivacyScope
import com.strhodler.utxopocket.domain.privacy.PrivacySeverity
import com.strhodler.utxopocket.domain.privacy.PrivacySummary
import com.strhodler.utxopocket.domain.privacy.PrivacySummaryEntry
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalletDetailUiReducerPrivacyTest {

    private val reducer = WalletDetailUiReducer(
        utxoVisualizationCalculator = UtxoVisualizationCalculator(),
        utxoTreemapCalculator = UtxoTreemapCalculator()
    )

    @Test
    fun reduceKeepsPrivacyEmptyWhenReducerHandlesLoadingLikeNullDetailState() {
        val state = reducer.reduce(
            baseInput(
                detail = null,
                syncStatus = SyncStatusSnapshot(
                    isRefreshing = true,
                    network = BitcoinNetwork.TESTNET
                )
            )
        )

        assertEquals(true, state.isRefreshing)
        assertEquals(PrivacySummary.Empty, state.walletPrivacySummary)
        assertTrue(state.walletPrivacyFindings.isEmpty())
    }

    @Test
    fun reduceUsesEmptyPrivacyStateWhenWalletDetailIsMissing() {
        val state = reducer.reduce(baseInput(detail = null))

        assertEquals(PrivacySummary.Empty, state.walletPrivacySummary)
        assertTrue(state.walletPrivacyFindings.isEmpty())
    }

    @Test
    fun reduceKeepsEmptyPrivacyStateWhenDetailHasNoFindings() {
        val detail = walletDetail()

        val state = reducer.reduce(
            baseInput(
                detail = detail,
                walletPrivacySummary = PrivacySummary.Empty,
                walletPrivacyFindings = emptyList()
            )
        )

        assertEquals(PrivacySummary.Empty, state.walletPrivacySummary)
        assertTrue(state.walletPrivacyFindings.isEmpty())
    }

    @Test
    fun reduceExposesProvidedWalletPrivacySummaryAndFindings() {
        val findings = listOf(
            PrivacyFinding(
                id = "wallet-address-reuse",
                scope = PrivacyScope.Wallet,
                severity = PrivacySeverity.Warning,
                confidence = PrivacyConfidence.High,
                evidence = mapOf("reused_address_count" to "2")
            )
        )
        val summary = PrivacySummary(
            entries = listOf(
                PrivacySummaryEntry(
                    severity = PrivacySeverity.Warning,
                    count = 1,
                    findingIds = listOf("wallet-address-reuse")
                )
            )
        )

        val state = reducer.reduce(
            baseInput(
                detail = walletDetail(),
                walletPrivacySummary = summary,
                walletPrivacyFindings = findings
            )
        )

        assertEquals(summary, state.walletPrivacySummary)
        assertEquals(findings, state.walletPrivacyFindings)
    }

    private fun baseInput(
        detail: WalletDetail?,
        syncStatus: SyncStatusSnapshot = SyncStatusSnapshot(
            isRefreshing = false,
            network = BitcoinNetwork.TESTNET
        ),
        walletPrivacySummary: PrivacySummary = PrivacySummary.Empty,
        walletPrivacyFindings: List<PrivacyFinding> = emptyList()
    ): WalletDetailUiReducerInput = WalletDetailUiReducerInput(
        detail = detail,
        canvasSnapshot = UtxoCanvasSnapshot(
            collections = emptyList(),
            memberships = emptyList(),
            items = emptyList()
        ),
        nodeSnapshot = NodeStatusSnapshot(
            status = NodeStatus.Synced,
            network = BitcoinNetwork.TESTNET,
            blockHeight = 200_000L
        ),
        syncStatus = syncStatus,
        torStatus = TorStatus.Stopped,
        balanceUnit = BalanceUnit.SATS,
        balancesHidden = false,
        hapticsEnabled = true,
        advancedMode = false,
        dustThresholdSats = 546L,
        balanceHistory = emptyList(),
        displayBalancePoints = emptyList(),
        pinLockEnabled = false,
        pinShuffleEnabled = false,
        transactionSort = WalletTransactionSort.NEWEST_FIRST,
        showPending = false,
        utxoSort = WalletUtxoSort.LARGEST_AMOUNT,
        selectedRange = BalanceRange.All,
        utxoLabelFilter = UtxoLabelFilter(),
        transactionLabelFilter = TransactionLabelFilter(),
        showBalanceChart = false,
        incomingPlaceholders = emptyList(),
        syncGap = null,
        utxoHistogramMode = UtxoHistogramMode.Count,
        utxoTreemapColorMode = UtxoTreemapColorMode.Age,
        utxoTreemapRange = null,
        utxoTreemapRequested = true,
        walletPrivacySummary = walletPrivacySummary,
        walletPrivacyFindings = walletPrivacyFindings
    )

    private fun walletDetail(): WalletDetail = WalletDetail(
        summary = WalletSummary(
            id = 1L,
            name = "Privacy reducer wallet",
            balanceSats = 120_000L,
            transactionCount = 1,
            utxoCount = 1,
            network = BitcoinNetwork.TESTNET,
            lastSyncStatus = NodeStatus.Synced,
            lastSyncTime = null
        ),
        descriptor = "wpkh(test)",
        transactions = listOf(
            WalletTransaction(
                id = "tx-1",
                amountSats = 120_000L,
                timestamp = 1_700_000_000_000L,
                type = TransactionType.RECEIVED,
                confirmations = 6
            )
        ),
        utxos = listOf(
            WalletUtxo(
                txid = "tx-1",
                vout = 0,
                valueSats = 120_000L,
                confirmations = 6,
                spendable = true
            )
        )
    )
}
