package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransportPolicyTest {

    @Test
    fun torOnlyPolicyResolvesToTorTransport() {
        assertEquals(NodeTransport.TOR, TransportPolicy.TOR_ONLY.resolveTransportOrNull())
    }

    @Test
    fun vpnDirectPolicyResolvesToDirectTransport() {
        assertEquals(NodeTransport.VPN_DIRECT, TransportPolicy.VPN_DIRECT_REQUIRED.resolveTransportOrNull())
    }

    @Test
    fun connectionModeMapsToExpectedTransportPolicy() {
        assertEquals(
            TransportPolicy.TOR_ONLY,
            TransportPolicy.forConnectionMode(ConnectionMode.TOR_DEFAULT)
        )
        assertEquals(
            TransportPolicy.VPN_DIRECT_REQUIRED,
            TransportPolicy.forConnectionMode(ConnectionMode.LOCAL_DIRECT)
        )
    }

    @Test
    fun defaultPolicyKeepsCurrentTorOnlyBehavior() {
        assertEquals(TransportPolicy.TOR_ONLY, TransportPolicy.default())
        assertEquals(NodeTransport.TOR, TransportPolicy.default().resolveTransportOrNull())
        assertTrue(NodeTransport.entries.contains(NodeTransport.VPN_DIRECT))
    }
}
