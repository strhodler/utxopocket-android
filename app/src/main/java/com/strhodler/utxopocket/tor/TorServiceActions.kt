package com.strhodler.utxopocket.tor

object TorServiceActions {
    const val ACTION_START = "com.strhodler.utxopocket.tor.action.START"
    const val ACTION_STOP = "com.strhodler.utxopocket.tor.action.STOP"
    const val ACTION_RENEW = "com.strhodler.utxopocket.tor.action.RENEW_IDENTITY"
    const val ACTION_INIT = "com.strhodler.utxopocket.tor.action.INIT"

    const val TOR_CHANNEL_ID = "tor_status_channel"
    const val TOR_NOTIFICATION_ID = 95
}
