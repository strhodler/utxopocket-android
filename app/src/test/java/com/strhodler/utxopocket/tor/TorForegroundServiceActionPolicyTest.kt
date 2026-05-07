package com.strhodler.utxopocket.tor

import kotlin.test.Test
import kotlin.test.assertEquals

class TorForegroundServiceActionPolicyTest {

    @Test
    fun startCommandAcceptsStartAndInitOnly() {
        assertEquals(
            TorServiceCommand.START,
            resolveTorServiceCommand(TorServiceActions.ACTION_START)
        )
        assertEquals(
            TorServiceCommand.START,
            resolveTorServiceCommand(TorServiceActions.ACTION_INIT)
        )
    }

    @Test
    fun stopAndRenewCommandsMapToExpectedActions() {
        assertEquals(
            TorServiceCommand.STOP,
            resolveTorServiceCommand(TorServiceActions.ACTION_STOP)
        )
        assertEquals(
            TorServiceCommand.RENEW,
            resolveTorServiceCommand(TorServiceActions.ACTION_RENEW)
        )
    }

    @Test
    fun unknownOrNullActionsAreIgnoredToAvoidImplicitTorStarts() {
        assertEquals(TorServiceCommand.IGNORE, resolveTorServiceCommand(null))
        assertEquals(
            TorServiceCommand.IGNORE,
            resolveTorServiceCommand("com.strhodler.utxopocket.tor.action.UNKNOWN")
        )
    }
}
