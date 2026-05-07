package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.model.NodeTransport
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class NodeSyncRetryPolicyTest {

    @Test
    fun boundedRetryContinuesOnlyBeforeLastAttempt() {
        assertTrue(shouldRetryAttempt(attempt = 0, maxAttempts = 3))
        assertTrue(shouldRetryAttempt(attempt = 1, maxAttempts = 3))
        assertFalse(shouldRetryAttempt(attempt = 2, maxAttempts = 3))
    }

    @Test
    fun exhaustedAttemptsDoNotResetForPresetRotation() {
        assertFalse(shouldRetryAttempt(attempt = 1, maxAttempts = 2))
    }

    @Test
    fun maxAttemptsMustBePositive() {
        assertFailsWith<IllegalArgumentException> {
            shouldRetryAttempt(attempt = 0, maxAttempts = 0)
        }
    }

    @Test
    fun torOnlyPolicyAllowsOnlyTorTransport() {
        assertTrue(isTransportAllowedByPolicy(NodeTransport.TOR))
        assertFalse(isTransportAllowedByPolicy(NodeTransport.VPN_DIRECT))
    }

    @Test
    fun vpnDirectPolicyAllowsOnlyDirectTransport() {
        assertTrue(
            isTransportAllowedByPolicy(
                transport = NodeTransport.VPN_DIRECT,
                policy = TransportPolicy.VPN_DIRECT_REQUIRED
            )
        )
        assertFalse(
            isTransportAllowedByPolicy(
                transport = NodeTransport.TOR,
                policy = TransportPolicy.VPN_DIRECT_REQUIRED
            )
        )
    }
}
