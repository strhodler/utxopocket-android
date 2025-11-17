package com.strhodler.utxopocket.presentation.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NodeQrParserTest {

    @Test
    fun `parses host and port`() {
        val result = parseNodeQrContent("umbrel.local:50001")

        val hostPort = assertIs<NodeQrParseResult.HostPort>(result)
        assertEquals("umbrel.local", hostPort.host)
        assertEquals("50001", hostPort.port)
        assertTrue(hostPort.useSsl)
    }

    @Test
    fun `parses host port with protocol suffix`() {
        val result = parseNodeQrContent("umbrel.local:50001:t")

        val hostPort = assertIs<NodeQrParseResult.HostPort>(result)
        assertEquals("umbrel.local", hostPort.host)
        assertEquals("50001", hostPort.port)
        assertTrue(!hostPort.useSsl)
    }

    @Test
    fun `parses scheme prefixed endpoint`() {
        val result = parseNodeQrContent("ssl://node.example.com:50002?tls=1")

        val hostPort = assertIs<NodeQrParseResult.HostPort>(result)
        assertEquals("node.example.com", hostPort.host)
        assertEquals("50002", hostPort.port)
        assertTrue(hostPort.useSsl)
    }

    @Test
    fun `parses tcp scheme as non ssl`() {
        val result = parseNodeQrContent("tcp://node.example.com:50001")

        val hostPort = assertIs<NodeQrParseResult.HostPort>(result)
        assertEquals("node.example.com", hostPort.host)
        assertEquals("50001", hostPort.port)
        assertTrue(!hostPort.useSsl)
    }

    @Test
    fun `electrum scheme keeps ssl enabled by default`() {
        val result = parseNodeQrContent("electrum://node.example.com:60002#metadata")

        val hostPort = assertIs<NodeQrParseResult.HostPort>(result)
        assertEquals("node.example.com", hostPort.host)
        assertEquals("60002", hostPort.port)
        assertTrue(hostPort.useSsl)
    }

    @Test
    fun `parses onion without port`() {
        val result = parseNodeQrContent("example123.onion")

        val onion = assertIs<NodeQrParseResult.Onion>(result)
        assertEquals("example123.onion", onion.address)
    }

    @Test
    fun `parses onion with port`() {
        val result = parseNodeQrContent("example123.onion:50001")

        val onion = assertIs<NodeQrParseResult.Onion>(result)
        assertEquals("example123.onion:50001", onion.address)
    }

    @Test
    fun `handles empty payload`() {
        val result = parseNodeQrContent("   ")

        val error = assertIs<NodeQrParseResult.Error>(result)
        assertTrue(error.reason.contains("empty", ignoreCase = true))
    }

    @Test
    fun `invalid port yields error`() {
        val result = parseNodeQrContent("example.com:abc")

        val error = assertIs<NodeQrParseResult.Error>(result)
        assertTrue(error.reason.contains("port", ignoreCase = true))
    }
}
