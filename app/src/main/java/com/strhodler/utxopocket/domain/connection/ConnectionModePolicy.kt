package com.strhodler.utxopocket.domain.connection

object ConnectionModeErrorKeys {
    const val INCOMPATIBLE_ENDPOINT = "connection_mode_incompatible_endpoint"
    const val REQUIRES_TOR = "connection_mode_requires_tor"
    const val REQUIRES_LOCAL_IP_LITERAL = "connection_mode_requires_local_ip_literal"
    const val REQUIRES_TCP = "connection_mode_requires_tcp"
    const val NO_FALLBACK_APPLIED = "connection_mode_no_fallback_applied"
}

class ConnectionModePolicyException(
    val errorKey: String
) : IllegalStateException(errorKey)
