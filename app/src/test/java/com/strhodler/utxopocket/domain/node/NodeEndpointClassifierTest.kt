package com.strhodler.utxopocket.domain.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    @Test
    fun detectsLocalIpv6Addresses() {
        val normalized = NodeEndpointClassifier.normalize("ssl://[fd12:3456::1]:50002")
        assertEquals(EndpointKind.LOCAL, normalized.kind)
        assertEquals("fd12:3456::1", normalized.host)
        assertEquals("ssl://[fd12:3456::1]:50002", normalized.url)
    }

    @Test
    fun treatsMdnsHostnamesAsPublic() {
        val normalized = NodeEndpointClassifier.normalize("ssl://mynode.local:50002")
        assertEquals(EndpointKind.PUBLIC, normalized.kind)
    }

    @Test
    fun localIpLiteralValidationRejectsHostnames() {
        assertTrue(NodeEndpointClassifier.isLocalIpLiteral("192.168.1.10"))
        assertTrue(NodeEndpointClassifier.isLocalIpLiteral("fd12:3456::1"))
        assertFalse(NodeEndpointClassifier.isLocalIpLiteral("localhost"))
        assertFalse(NodeEndpointClassifier.isLocalIpLiteral("mynode.local"))
    }

    @Test
    fun rejectsMalformedPorts() {
        assertFailsWith<IllegalArgumentException> {
            NodeEndpointClassifier.normalize("ssl://example.com:notaport")
        }
        assertFailsWith<IllegalArgumentException> {
            NodeEndpointClassifier.normalize("ssl://example.com:50002:extra")
        }
        assertFailsWith<IllegalArgumentException> {
            NodeEndpointClassifier.normalize("ssl://[fd12::1]junk")
        }
        assertFailsWith<IllegalArgumentException> {
            NodeEndpointClassifier.normalize("ssl://[fd12::1]:notaport")
        }
    }

    @Test
    fun rejectsOutOfRangeIpv4Literals() {
        assertFailsWith<IllegalArgumentException> {
            NodeEndpointClassifier.normalize("ssl://192.168.1.999:50002")
        }
        assertFailsWith<IllegalArgumentException> {
            NodeEndpointClassifier.normalize("ssl://999.168.1.1:50002")
        }
    }
}
