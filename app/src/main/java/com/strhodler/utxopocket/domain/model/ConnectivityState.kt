package com.strhodler.utxopocket.domain.model

/**
 * Snapshot of the device connectivity transports that matter for custom node reachability.
 */
data class ConnectivityState(
    val isOnline: Boolean = false,
    val onLocalNetwork: Boolean = false,
    val onVpn: Boolean = false,
    val onCellular: Boolean = false
)
