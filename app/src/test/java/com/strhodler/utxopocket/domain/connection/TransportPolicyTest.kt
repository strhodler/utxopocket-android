package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.NodeTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransportPolicyTest {

    @Test
    fun torOnlyPolicyResolvesToTorTransport() {
        assertEquals(NodeTransport.TOR, TransportPolicy.TOR_ONLY.resolveTransportOrNull())
    }

    @Test
    fun vpnDirectPolicyIsFailClosedUntilEnabled() {
        assertNull(TransportPolicy.VPN_DIRECT_REQUIRED.resolveTransportOrNull())
    }

    @Test
    fun defaultPolicyKeepsCurrentTorOnlyBehavior() {
        assertEquals(TransportPolicy.TOR_ONLY, TransportPolicy.default())
        assertEquals(NodeTransport.TOR, TransportPolicy.default().resolveTransportOrNull())
        assertTrue(NodeTransport.entries.contains(NodeTransport.VPN_DIRECT))
    }
}
