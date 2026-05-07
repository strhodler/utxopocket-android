package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
    fun connectionIntentsRemainDistinctSingletons() {
        assertEquals(ConnectionIntent.Start, ConnectionIntent.Start)
        assertEquals(ConnectionIntent.Retry, ConnectionIntent.Retry)
        assertEquals(ConnectionIntent.Disconnect, ConnectionIntent.Disconnect)
        assertEquals(ConnectionIntent.OnAppForeground, ConnectionIntent.OnAppForeground)
        assertFalse(ConnectionIntent.Start == ConnectionIntent.Retry)
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
