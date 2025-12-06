package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class IncomingTxCoordinator @Inject constructor() {

    private val _placeholders = MutableStateFlow<Map<Long, List<IncomingTxPlaceholder>>>(emptyMap())
    val placeholders: StateFlow<Map<Long, List<IncomingTxPlaceholder>>> = _placeholders.asStateFlow()

    private val _detections = MutableSharedFlow<IncomingTxDetection>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val detections: SharedFlow<IncomingTxDetection> = _detections.asSharedFlow()

    private val lastDialogAt = mutableMapOf<Long, Long>()

    fun onDetection(event: IncomingTxDetection) {
        addPlaceholder(event)
        val now = System.currentTimeMillis()
        val lastShown = lastDialogAt[event.walletId] ?: 0L
        if (now - lastShown >= DIALOG_THROTTLE_MS) {
            lastDialogAt[event.walletId] = now
            _detections.tryEmit(event)
        }
    }

    fun markResolved(walletId: Long, txid: String) {
        _placeholders.value[walletId]?.let { existing ->
            val filtered = existing.filterNot { it.txid == txid }
            updatePlaceholders(walletId, filtered)
        }
    }

    fun clearWallet(walletId: Long) {
        updatePlaceholders(walletId, emptyList())
    }

    private fun addPlaceholder(event: IncomingTxDetection) {
        val next = _placeholders.value.toMutableMap()
        val current = next[event.walletId].orEmpty()
        if (current.any { it.txid == event.txid }) {
            return
        }
        val updated = (listOf(
            IncomingTxPlaceholder(
                txid = event.txid,
                address = event.address,
                amountSats = event.amountSats,
                detectedAt = event.detectedAt
            )
        ) + current).sortedByDescending { it.detectedAt }
        next[event.walletId] = updated
        _placeholders.value = next
    }

    private fun updatePlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        val next = _placeholders.value.toMutableMap()
        if (placeholders.isEmpty()) {
            next.remove(walletId)
        } else {
            next[walletId] = placeholders
        }
        _placeholders.value = next
    }

    companion object {
        private const val DIALOG_THROTTLE_MS = 10_000L
    }
}
