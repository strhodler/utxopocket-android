package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import kotlinx.coroutines.flow.StateFlow

interface ConnectionOrchestrator {
    val snapshot: StateFlow<ConnectionSnapshot>

    fun onIntent(intent: ConnectionIntent)
}
