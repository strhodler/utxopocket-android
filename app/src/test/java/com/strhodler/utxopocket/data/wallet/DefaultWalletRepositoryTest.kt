package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncQueueEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultWalletRepositoryTest {

    @Test
    fun originsAreCompatibleRegardlessOfHardenedNotation() {
        val labelOrigin = "wpkh([8e8074b3/84h/1h/0h])"
        val walletOrigin = "wpkh([8e8074b3/84'/1'/0'])"

        assertTrue(DefaultWalletRepository.originsCompatible(labelOrigin, walletOrigin))
    }

    @Test
    fun prepareReenqueueChunksFiltersDeletedAndMissing() {
        val drained = listOf(
            SyncQueueEntry(walletId = 10L, operation = SyncOperation.Refresh),
            SyncQueueEntry(walletId = 11L, operation = SyncOperation.FullRescan),
            SyncQueueEntry(walletId = 12L, operation = SyncOperation.Refresh)
        )
        val remaining = setOf(11L, 12L)

        val result = DefaultWalletRepository.prepareReenqueueChunks(
            drainedEntries = drained,
            deletedWalletId = 10L,
            remainingWalletIds = remaining
        )

        val expected = listOf(
            DefaultWalletRepository.ReenqueueChunk(
                operation = SyncOperation.FullRescan,
                walletIds = listOf(11L)
            ),
            DefaultWalletRepository.ReenqueueChunk(
                operation = SyncOperation.Refresh,
                walletIds = listOf(12L)
            )
        )

        assertEquals(expected, result)
    }

    @Test
    fun prepareReenqueueChunksGroupsByOperation() {
        val drained = listOf(
            SyncQueueEntry(walletId = 1L, operation = SyncOperation.Refresh),
            SyncQueueEntry(walletId = 2L, operation = SyncOperation.Refresh),
            SyncQueueEntry(walletId = 3L, operation = SyncOperation.FullRescan),
            SyncQueueEntry(walletId = 4L, operation = SyncOperation.FullRescan),
            SyncQueueEntry(walletId = 5L, operation = SyncOperation.Refresh)
        )
        val remaining = setOf(1L, 2L, 3L, 4L, 5L)

        val result = DefaultWalletRepository.prepareReenqueueChunks(
            drainedEntries = drained,
            deletedWalletId = 99L,
            remainingWalletIds = remaining
        )

        val expected = listOf(
            DefaultWalletRepository.ReenqueueChunk(
                operation = SyncOperation.Refresh,
                walletIds = listOf(1L, 2L)
            ),
            DefaultWalletRepository.ReenqueueChunk(
                operation = SyncOperation.FullRescan,
                walletIds = listOf(3L, 4L)
            ),
            DefaultWalletRepository.ReenqueueChunk(
                operation = SyncOperation.Refresh,
                walletIds = listOf(5L)
            )
        )

        assertEquals(expected, result)
    }

    @Test
    fun originsWithDifferentFingerprintsAreNotCompatible() {
        val labelOrigin = "wpkh([deadbeef/84h/1h/0h])"
        val walletOrigin = "wpkh([8e8074b3/84'/1'/0'])"

        assertFalse(DefaultWalletRepository.originsCompatible(labelOrigin, walletOrigin))
    }

    @Test
    fun originsWithFullDescriptorPayloadAreCompatible() {
        val labelOrigin =
            "wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/<0;1>/*)"
        val walletOrigin = "wpkh([8e8074b3/84'/1'/0'])"

        assertTrue(DefaultWalletRepository.originsCompatible(labelOrigin, walletOrigin))
    }
}
