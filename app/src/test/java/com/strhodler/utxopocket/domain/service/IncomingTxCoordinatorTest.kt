package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class IncomingTxCoordinatorTest {

    @Test
    fun sameTxidUpdatesExistingPlaceholderWithoutDuplication() = runTest {
        val repository = CoordinatorTestPlaceholderRepository()
        val coordinator = IncomingTxCoordinator(
            placeholderRepository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        coordinator.onDetection(
            IncomingTxDetection(
                walletId = 1L,
                address = "tb1qaddress",
                derivationIndex = 5,
                txid = "tx-1",
                amountSats = 100,
                lightStatus = IncomingTxLightStatus.UNCONFIRMED,
                detectedAt = 10
            )
        )
        coordinator.onDetection(
            IncomingTxDetection(
                walletId = 1L,
                address = "tb1qaddress",
                derivationIndex = 5,
                txid = "tx-1",
                amountSats = 100,
                lightStatus = IncomingTxLightStatus.CONFIRMED_LIGHT,
                lastSeenHeight = 900,
                detectedAt = 20
            )
        )

        val placeholders = coordinator.placeholders.value[1L].orEmpty()
        assertEquals(1, placeholders.size)
        assertEquals(IncomingTxLightStatus.CONFIRMED_LIGHT, placeholders.first().lightStatus)
        assertEquals(900L, placeholders.first().lastSeenHeight)
        assertEquals(10L, placeholders.first().detectedAt)
    }

    @Test
    fun repeatedDetectionWithSameStateIsIdempotent() = runTest {
        val repository = CoordinatorTestPlaceholderRepository()
        val coordinator = IncomingTxCoordinator(
            placeholderRepository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val event = IncomingTxDetection(
            walletId = 1L,
            address = "tb1qidempotent",
            derivationIndex = 1,
            txid = "tx-repeat",
            amountSats = 200,
            lightStatus = IncomingTxLightStatus.UNCONFIRMED,
            detectedAt = 100
        )

        coordinator.onDetection(event)
        coordinator.onDetection(event)

        val placeholders = coordinator.placeholders.value[1L].orEmpty()
        assertEquals(1, placeholders.size)
        assertEquals("tx-repeat", placeholders.first().txid)
    }

    @Test
    fun reconcileWithCanonicalTxidsRemovesOnlyMatchedPlaceholders() = runTest {
        val repository = CoordinatorTestPlaceholderRepository()
        val coordinator = IncomingTxCoordinator(
            placeholderRepository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        coordinator.onDetection(
            IncomingTxDetection(
                walletId = 9L,
                address = "tb1qa",
                derivationIndex = 0,
                txid = "tx-a",
                amountSats = 300
            )
        )
        coordinator.onDetection(
            IncomingTxDetection(
                walletId = 9L,
                address = "tb1qb",
                derivationIndex = 1,
                txid = "tx-b",
                amountSats = 400
            )
        )

        coordinator.reconcileWithCanonicalTxids(
            walletId = 9L,
            canonicalTxids = setOf("tx-a")
        )

        val placeholders = coordinator.placeholders.value[9L].orEmpty()
        assertEquals(1, placeholders.size)
        assertEquals("tx-b", placeholders.first().txid)
    }
}

private class CoordinatorTestPlaceholderRepository : IncomingTxPlaceholderRepository {
    private val state = MutableStateFlow<Map<Long, List<IncomingTxPlaceholder>>>(emptyMap())

    override val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>> = state

    override suspend fun setPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        val next = state.value.toMutableMap()
        if (placeholders.isEmpty()) {
            next.remove(walletId)
        } else {
            next[walletId] = placeholders
        }
        state.value = next
    }
}
