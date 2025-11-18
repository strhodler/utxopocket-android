package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.ConnectivityState
import kotlinx.coroutines.flow.StateFlow

interface ConnectivityMonitor {
    val state: StateFlow<ConnectivityState>
}
