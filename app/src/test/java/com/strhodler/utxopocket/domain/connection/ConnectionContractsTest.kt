package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionContractsTest {

    @Test
    fun connectionStatesExposeStableOrder() {
        assertEquals(
            listOf(
                ConnectionState.IDLE,
                ConnectionState.CONNECTING,
                ConnectionState.CONNECTED,
                ConnectionState.DISCONNECTED,
                ConnectionState.ERROR
            ),
            ConnectionState.entries.toList()
        )
    }

    @Test
    fun networkChangedIntentCarriesConnectivitySignal() {
        assertTrue((ConnectionIntent.OnNetworkChanged(isOnline = true)).isOnline)
        assertFalse((ConnectionIntent.OnNetworkChanged(isOnline = false)).isOnline)
    }

    @Test
    fun snapshotDefaultsAreIdleAndTorOnly() {
        val snapshot = ConnectionSnapshot()

        assertEquals(ConnectionState.IDLE, snapshot.state)
        assertEquals(BitcoinNetwork.DEFAULT, snapshot.network)
        assertFalse(snapshot.isConnected)
        assertFalse(snapshot.canRetry)
    }
}
