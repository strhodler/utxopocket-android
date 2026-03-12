package com.strhodler.utxopocket.data.tor

import com.strhodler.utxopocket.tor.TorRuntimeManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultTorManagerStartActionPolicyTest {

    @Test
    fun sendStartAction_whenRuntimeIsIdle() {
        assertTrue(shouldSendTorStartAction(TorRuntimeManager.ConnectionState.IDLE))
    }

    @Test
    fun sendStartAction_whenRuntimeIsDisconnected() {
        assertTrue(shouldSendTorStartAction(TorRuntimeManager.ConnectionState.DISCONNECTED))
    }

    @Test
    fun sendStartAction_whenRuntimeIsError() {
        assertTrue(shouldSendTorStartAction(TorRuntimeManager.ConnectionState.ERROR))
    }

    @Test
    fun doNotSendStartAction_whenRuntimeIsConnecting() {
        assertFalse(shouldSendTorStartAction(TorRuntimeManager.ConnectionState.CONNECTING))
    }

    @Test
    fun doNotSendStartAction_whenRuntimeIsConnected() {
        assertFalse(shouldSendTorStartAction(TorRuntimeManager.ConnectionState.CONNECTED))
    }
}
