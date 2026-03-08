package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class IncomingTxCoordinator @Inject constructor(
    private val placeholderRepository: IncomingTxPlaceholderRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _placeholders = MutableStateFlow<Map<Long, List<IncomingTxPlaceholder>>>(emptyMap())
    val placeholders: StateFlow<Map<Long, List<IncomingTxPlaceholder>>> = _placeholders.asStateFlow()

    private val _sheetTriggers = MutableSharedFlow<IncomingTxDetection>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sheetTriggers: SharedFlow<IncomingTxDetection> = _sheetTriggers.asSharedFlow()

    private val lastTriggerAt = mutableMapOf<Long, Long>()

    init {
        scope.launch {
            placeholderRepository.placeholders.collect { stored ->
                _placeholders.value = stored
            }
        }
    }

    fun onDetection(event: IncomingTxDetection) {
        addPlaceholder(event)
        val now = System.currentTimeMillis()
        val lastShown = lastTriggerAt[event.walletId] ?: 0L
        if (now - lastShown >= SHEET_TRIGGER_THROTTLE_MS) {
            lastTriggerAt[event.walletId] = now
            _sheetTriggers.tryEmit(event)
        }
    }

    fun markResolved(walletId: Long, txid: String) {
        reconcileWithCanonicalTxids(walletId, setOf(txid))
    }

    fun reconcileWithCanonicalTxids(walletId: Long, canonicalTxids: Set<String>) {
        if (canonicalTxids.isEmpty()) return
        _placeholders.value[walletId]?.let { existing ->
            val filtered = existing.filterNot { placeholder ->
                canonicalTxids.contains(placeholder.txid)
            }
            if (filtered.size == existing.size) return
            updatePlaceholders(walletId, filtered)
        }
    }

    fun clearWallet(walletId: Long) {
        updatePlaceholders(walletId, emptyList())
    }

    private fun addPlaceholder(event: IncomingTxDetection) {
        val currentState = _placeholders.value
        val current = currentState[event.walletId].orEmpty()
        val existingIndex = current.indexOfFirst { it.txid == event.txid }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val merged = existing.mergeWith(event)
            if (merged == existing) return
            val updated = current.toMutableList().apply { set(existingIndex, merged) }
                .sortedByDescending { it.detectedAt }
            updatePlaceholders(event.walletId, updated)
            return
        }
        val updated = (listOf(
            IncomingTxPlaceholder(
                txid = event.txid,
                address = event.address,
                amountSats = event.amountSats,
                lightStatus = event.lightStatus,
                lastSeenHeight = event.lastSeenHeight,
                detectedAt = event.detectedAt
            )
        ) + current).sortedByDescending { it.detectedAt }
        updatePlaceholders(event.walletId, updated)
    }

    private fun IncomingTxPlaceholder.mergeWith(event: IncomingTxDetection): IncomingTxPlaceholder {
        val mergedStatus = if (
            lightStatus == IncomingTxLightStatus.CONFIRMED_LIGHT ||
            event.lightStatus == IncomingTxLightStatus.CONFIRMED_LIGHT
        ) {
            IncomingTxLightStatus.CONFIRMED_LIGHT
        } else {
            IncomingTxLightStatus.UNCONFIRMED
        }
        val mergedHeight = listOfNotNull(lastSeenHeight, event.lastSeenHeight).maxOrNull()
        return copy(
            amountSats = amountSats ?: event.amountSats,
            lightStatus = mergedStatus,
            lastSeenHeight = mergedHeight
        )
    }

    private fun updatePlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        val next = _placeholders.value.toMutableMap()
        if (placeholders.isEmpty()) {
            next.remove(walletId)
        } else {
            next[walletId] = placeholders
        }
        _placeholders.value = next
        persistPlaceholders(walletId, placeholders)
    }

    private fun persistPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        scope.launch {
            placeholderRepository.setPlaceholders(walletId, placeholders)
        }
    }

    companion object {
        private const val SHEET_TRIGGER_THROTTLE_MS = 10_000L
    }
}
