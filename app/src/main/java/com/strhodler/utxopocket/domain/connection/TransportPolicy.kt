package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeTransport

enum class TransportPolicy {
    TOR_ONLY,
    VPN_DIRECT_REQUIRED;

    fun resolveTransportOrNull(): NodeTransport =
        when (this) {
            TOR_ONLY -> NodeTransport.TOR
            VPN_DIRECT_REQUIRED -> NodeTransport.VPN_DIRECT
        }

    companion object {
        fun forConnectionMode(connectionMode: ConnectionMode): TransportPolicy =
            when (connectionMode) {
                ConnectionMode.TOR_DEFAULT -> TOR_ONLY
                ConnectionMode.LOCAL_DIRECT -> VPN_DIRECT_REQUIRED
            }

        fun default(): TransportPolicy = TOR_ONLY
    }
}
