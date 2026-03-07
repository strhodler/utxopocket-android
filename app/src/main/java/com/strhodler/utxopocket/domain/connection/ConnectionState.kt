package com.strhodler.utxopocket.domain.connection

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}
