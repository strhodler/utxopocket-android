package com.strhodler.utxopocket.domain.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeEndpointClassifierTest {

    @Test
    fun normalizesHostPortEndpoints() {
        val normalized = NodeEndpointClassifier.normalize("SSL://Example.com:50002")
        assertEquals(EndpointScheme.SSL, normalized.scheme)
        assertEquals("example.com", normalized.host)
        assertEquals(50002, normalized.port)
        assertEquals(EndpointKind.PUBLIC, normalized.kind)
        assertEquals("ssl://example.com:50002", normalized.url)
    }

    @Test
    fun detectsOnionEndpoints() {
        val normalized = NodeEndpointClassifier.normalize("tcp://abc123def.onion:50001")
        assertEquals(EndpointKind.ONION, normalized.kind)
        assertEquals(EndpointScheme.TCP, normalized.scheme)
        assertEquals("abc123def.onion", normalized.host)
        assertEquals("tcp://abc123def.onion:50001", normalized.url)
    }

    @Test
    fun detectsLocalIpv4Addresses() {
        val normalized = NodeEndpointClassifier.normalize("ssl://192.168.1.10:60002")
        assertEquals(EndpointKind.LOCAL, normalized.kind)
        assertTrue(NodeEndpointClassifier.detectKind("192.168.1.10") == EndpointKind.LOCAL)
    }
}
