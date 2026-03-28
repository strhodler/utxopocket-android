package com.strhodler.utxopocket.data.electrum

import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.node.EndpointScheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightElectrumClientSecurityPolicyTest {

    @Test
    fun shouldFailClosedForInsecureSsl_returnsTrue_whenSslWithoutValidation() {
        assertTrue(
            shouldFailClosedForInsecureSsl(
                endpointScheme = EndpointScheme.SSL,
                validateDomain = false
            )
        )
    }

    @Test
    fun shouldFailClosedForInsecureSsl_returnsFalse_whenSslWithValidation() {
        assertFalse(
            shouldFailClosedForInsecureSsl(
                endpointScheme = EndpointScheme.SSL,
                validateDomain = true
            )
        )
    }

    @Test
    fun shouldFailClosedForInsecureSsl_returnsFalse_whenTcp() {
        assertFalse(
            shouldFailClosedForInsecureSsl(
                endpointScheme = EndpointScheme.TCP,
                validateDomain = false
            )
        )
    }

    @Test
    fun shouldFailClosedForMissingTorProxy_returnsTrue_whenTorWithoutProxy() {
        assertTrue(
            shouldFailClosedForMissingTorProxy(
                endpointTransport = NodeTransport.TOR,
                proxy = null
            )
        )
    }

    @Test
    fun shouldFailClosedForMissingTorProxy_returnsFalse_whenTorWithProxy() {
        assertFalse(
            shouldFailClosedForMissingTorProxy(
                endpointTransport = NodeTransport.TOR,
                proxy = SocksProxyConfig(host = "127.0.0.1", port = 9050)
            )
        )
    }

    @Test
    fun shouldFailClosedForMissingTorProxy_returnsFalse_whenDirectTransport() {
        assertFalse(
            shouldFailClosedForMissingTorProxy(
                endpointTransport = NodeTransport.VPN_DIRECT,
                proxy = null
            )
        )
    }
}
