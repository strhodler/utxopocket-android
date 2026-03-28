package com.strhodler.utxopocket.data.electrum

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
}
