package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.NodeTransport

enum class TransportPolicy {
    TOR_ONLY,
    VPN_DIRECT_REQUIRED;

    fun resolveTransportOrNull(vpnDirectEnabled: Boolean = false): NodeTransport? =
        when (this) {
            TOR_ONLY -> NodeTransport.TOR
            VPN_DIRECT_REQUIRED -> if (vpnDirectEnabled) NodeTransport.VPN_DIRECT else null
        }

    companion object {
        fun default(): TransportPolicy = TOR_ONLY
    }
}
