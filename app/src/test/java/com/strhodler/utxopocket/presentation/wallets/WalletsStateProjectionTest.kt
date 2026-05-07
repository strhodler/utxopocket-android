package com.strhodler.utxopocket.presentation.wallets

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.presentation.wallets.sync.WalletSyncState
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletsStateProjectionTest {

    @Test
    fun transitionToDuressNeverProjectsRealWalletData() {
        val state = realWalletsState()

        val projected = projectWalletsForDuressTransition(
            state = state,
            duressState = DuressSessionState.FakeActive(decoyBalanceSats = 321_000L)
        )

        assertEquals(true, projected.duressActive)
        assertEquals(emptyList(), projected.wallets)
        assertEquals(0L, projected.totalBalanceSats)
        assertEquals(null, projected.blockHeight)
        assertEquals(null, projected.feeRateSatPerVb)
        assertEquals(null, projected.errorMessage)
        assertEquals(null, projected.connectionBannerModel)
        assertEquals(null, projected.connectedNodeLabel)
        assertEquals(emptySet<Long>(), projected.refreshingWalletIds)
        assertEquals(emptyList<Long>(), projected.queuedWalletIds)
        assertEquals(emptyMap<Long, SyncOperation>(), projected.queuedOperations)
        assertEquals(emptyMap<Long, WalletSyncState>(), projected.walletSyncStates)
        assertEquals(321_000L, projected.decoyBalanceSats)
    }

    @Test
    fun stateAlreadyInDuressRemainsUntouched() {
        val duressState = WalletsUiState(
            duressActive = true,
            decoyBalanceSats = 123_000L,
            wallets = listOf(fakeWallet())
        )

        val projected = projectWalletsForDuressTransition(
            state = duressState,
            duressState = DuressSessionState.FakeActive(decoyBalanceSats = 999_000L)
        )

        assertEquals(duressState, projected)
    }

    @Test
    fun inactiveDuressKeepsCurrentState() {
        val state = realWalletsState()

        val projected = projectWalletsForDuressTransition(
            state = state,
            duressState = DuressSessionState.Inactive
        )

        assertEquals(state, projected)
    }

    private fun realWalletsState(): WalletsUiState = WalletsUiState(
        wallets = listOf(realWallet()),
        totalBalanceSats = 777_000L,
        blockHeight = 123,
        feeRateSatPerVb = 2.1,
        errorMessage = "network error",
        connectedNodeLabel = "real-node",
        connectionBannerModel = WalletsConnectionBannerModel.NodeConnected(nodeLabel = "real-node"),
        refreshingWalletIds = setOf(99L),
        activeWalletId = 99L,
        queuedWalletIds = listOf(99L)
    )

    private fun realWallet() = WalletSummary(
        id = 99L,
        name = "Real Wallet",
        balanceSats = 777_000L,
        transactionCount = 5,
        network = BitcoinNetwork.TESTNET,
        lastSyncStatus = NodeStatus.Synced,
        lastSyncTime = 123L,
        descriptorType = DescriptorType.P2WPKH,
        viewOnly = true
    )

    private fun fakeWallet() = WalletSummary(
        id = -1L,
        name = "Wallet",
        balanceSats = 123_000L,
        transactionCount = 2,
        network = BitcoinNetwork.TESTNET,
        lastSyncStatus = NodeStatus.Synced,
        lastSyncTime = 456L,
        descriptorType = DescriptorType.P2WPKH,
        viewOnly = true
    )
}
