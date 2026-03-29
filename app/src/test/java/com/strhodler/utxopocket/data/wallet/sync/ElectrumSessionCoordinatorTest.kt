package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class ElectrumSessionCoordinatorTest {

    @Test
    fun torUnavailableReturnsWaitingForTorWithFallbackEndpoint() = runTest {
        var createSessionCalls = 0
        val coordinator = ElectrumSessionCoordinator(
            resolveEndpoint = { torEndpoint() },
            createSession = { _, _ ->
                createSessionCalls += 1
                throw AssertionError("Session must not be created when Tor proxy is unavailable")
            },
            withTorProxy = { _ -> throw IllegalStateException("Tor bootstrapping") },
            currentTorStatus = { TorStatus.Connecting(progress = 42, message = "Bootstrapping") }
        )

        val result = coordinator.runSessionEnvelope(
            endpoint = torEndpoint(),
            waitingEndpointLabel = "ssl://previous.onion:50002",
            block = { _, _ ->
                throw AssertionError("Sync block must not run when Tor proxy is unavailable")
            }
        )

        assertEquals(0, createSessionCalls)
        assertEquals(
            ElectrumSessionEnvelopeResult.WaitingForTor(
                endpointLabel = "ssl://previous.onion:50002",
                torStatus = TorStatus.Connecting(progress = 42, message = "Bootstrapping")
            ),
            result
        )
    }

    @Test
    fun policyMismatchReturnsDeterministicIncompatibleEndpointReason() = runTest {
        val coordinator = ElectrumSessionCoordinator(
            resolveEndpoint = {
                ElectrumEndpoint(
                    url = "tcp://192.168.1.10:50001",
                    validateDomain = false,
                    transport = NodeTransport.VPN_DIRECT
                )
            },
            createSession = { _, _ -> throw AssertionError("Not expected") },
            withTorProxy = { throw AssertionError("Not expected") },
            currentTorStatus = { null }
        )

        val resolution = coordinator.resolveEndpoint(
            network = BitcoinNetwork.TESTNET,
            connectionMode = ConnectionMode.TOR_DEFAULT
        )

        val mismatch = assertIs<ElectrumEndpointResolution.PolicyMismatch>(resolution)
        assertEquals(ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT, mismatch.reason)
    }

    @Test
    fun torRequiredDoesNotDowngradeToDirectWhenTorProxyUnavailable() = runTest {
        var attemptedDirectPath = false
        val coordinator = ElectrumSessionCoordinator(
            resolveEndpoint = { torEndpoint() },
            createSession = { _, proxy ->
                if (proxy == null) {
                    attemptedDirectPath = true
                }
                throw AssertionError("Session creation should not happen in this scenario")
            },
            withTorProxy = { _ -> throw IllegalStateException("No Tor proxy") },
            currentTorStatus = { TorStatus.Stopped }
        )

        val result = coordinator.runSessionEnvelope(
            endpoint = torEndpoint(),
            waitingEndpointLabel = null,
            block = { _, _ ->
                throw AssertionError("Sync block must not run when Tor proxy is unavailable")
            }
        )

        assertFalse(attemptedDirectPath)
        val waiting = assertIs<ElectrumSessionEnvelopeResult.WaitingForTor>(result)
        assertNull(waiting.endpointLabel)
        assertEquals(TorStatus.Stopped, waiting.torStatus)
    }

    private fun torEndpoint(): ElectrumEndpoint = ElectrumEndpoint(
        url = "ssl://electrum.exampleonionaddress.onion:50002",
        validateDomain = true,
        transport = NodeTransport.TOR
    )
}
