package com.strhodler.utxopocket.domain.connection

sealed interface ConnectionIntent {
    data object Start : ConnectionIntent
    data object Retry : ConnectionIntent
    data object Disconnect : ConnectionIntent
    data object OnAppForeground : ConnectionIntent
    data class OnNetworkChanged(val isOnline: Boolean) : ConnectionIntent
}
