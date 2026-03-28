package com.strhodler.utxopocket.data.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TorConnectionMessagesTest {

    @Test
    fun localEndpointIsRenderedAsTypeOnly() {
        val message = IllegalStateException("Connection failed").toTorAwareMessage(
            defaultMessage = "Unable to reach node",
            endpoint = "tcp://192.168.1.50:50001",
            usedTor = false
        )

        assertTrue(message.contains("local endpoint"))
        assertFalse(message.contains("192.168.1.50"))
        assertFalse(message.contains("50001"))
    }

    @Test
    fun onionEndpointIsRenderedAsTypeOnly() {
        val message = IllegalStateException("Connection failed").toTorAwareMessage(
            defaultMessage = "Unable to reach node",
            endpoint = "tcp://abcdef1234567890.onion:50001",
            usedTor = true
        )

        assertTrue(message.contains("onion endpoint"))
        assertFalse(message.contains("abcdef1234567890.onion"))
    }

    @Test
    fun noEndpointReturnsBaseMessage() {
        val message = IllegalStateException("Connection failed").toTorAwareMessage(
            defaultMessage = "Unable to reach node",
            endpoint = null,
            usedTor = false
        )

        assertEquals("Unable to reach node", message)
    }

    @Test
    fun defaultMessageRedactsIpv4HostPort() {
        val message = IllegalStateException("Connection failed").toTorAwareMessage(
            defaultMessage = "Failed to connect to tcp://192.168.1.50:50001",
            endpoint = "tcp://192.168.1.50:50001",
            usedTor = false
        )

        assertFalse(message.contains("192.168.1.50"))
        assertFalse(message.contains("50001"))
        assertTrue(message.contains("local endpoint"))
    }

    @Test
    fun defaultMessageRedactsIpv6HostPort() {
        val message = IllegalStateException("Connection failed").toTorAwareMessage(
            defaultMessage = "Failed to connect to tcp://[fd00::1]:50001",
            endpoint = "tcp://[fd00::1]:50001",
            usedTor = false
        )

        assertFalse(message.contains("fd00::1"))
        assertFalse(message.contains("50001"))
        assertTrue(message.contains("local endpoint"))
    }
}
