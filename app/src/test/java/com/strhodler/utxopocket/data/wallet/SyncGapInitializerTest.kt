package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class SyncGapInitializerTest {

    @Test
    fun seedsBaselineWhenMissing() = runTest {
        val prefs = RecordingSyncPrefs()
        val entity = baseEntity(network = BitcoinNetwork.MAINNET.name)

        SyncGapInitializer.seedSyncGapIfMissing(entity, prefs)

        assertEquals(WalletSyncPreferencesRepository.baseline(BitcoinNetwork.MAINNET), prefs.values[entity.id])
    }

    @Test
    fun doesNotOverrideExistingPreference() = runTest {
        val prefs = RecordingSyncPrefs(initial = mutableMapOf(1L to 150))
        val entity = baseEntity(id = 1L, network = BitcoinNetwork.TESTNET.name)

        SyncGapInitializer.seedSyncGapIfMissing(entity, prefs)

        assertEquals(150, prefs.values[entity.id])
    }

    private fun baseEntity(id: Long = 1L, network: String): WalletEntity =
        WalletEntity(
            id = id,
            name = "wallet",
            descriptor = "wpkh(desc)",
            network = network,
            balanceSats = 0,
            transactionCount = 0,
            lastSyncStatus = "Idle",
            lastSyncError = null
        )
}

private class RecordingSyncPrefs(
    val values: MutableMap<Long, Int> = mutableMapOf(),
    initial: MutableMap<Long, Int>? = null
) : WalletSyncPreferencesRepository {
    init {
        initial?.let { values.putAll(it) }
    }

    override suspend fun setGap(walletId: Long, gap: Int) {
        values[walletId] = gap
    }

    override suspend fun getGap(walletId: Long): Int? = values[walletId]

    override fun observeGap(walletId: Long) = kotlinx.coroutines.flow.flow { emit(values[walletId]) }
}
