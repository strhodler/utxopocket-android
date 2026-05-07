package com.strhodler.utxopocket.tor.control

import kotlin.test.Test
import kotlin.test.assertEquals

class TorServiceStatusParserTest {

    private val parser = TorServiceStatusParser()

    @Test
    fun parseRunningStatus_marksRunningWithFullBootstrap() {
        val parsed = parser.parseStatus(status = "ON")

        assertEquals(TorServiceRuntimeState.RUNNING, parsed.state)
        assertEquals(100, parsed.bootstrapPercent)
        assertEquals("ON", parsed.message)
    }

    @Test
    fun parseStoppingStatus_marksStoppingWithZeroBootstrap() {
        val parsed = parser.parseStatus(status = "STOPPING")

        assertEquals(TorServiceRuntimeState.STOPPING, parsed.state)
        assertEquals(0, parsed.bootstrapPercent)
        assertEquals("STOPPING", parsed.message)
    }

    @Test
    fun parseBootstrapMessage_extractsPercentAndKeepsStartingState() {
        val parsed = parser.parseStatus(status = "Bootstrapped 45%: Loading relay descriptors")

        assertEquals(TorServiceRuntimeState.STARTING, parsed.state)
        assertEquals(45, parsed.bootstrapPercent)
        assertEquals("Bootstrapped 45%: Loading relay descriptors", parsed.message)
    }

    @Test
    fun parseBootstrapAboveHundred_clampsAndTreatsAsRunning() {
        val parsed = parser.parseStatus(status = "Bootstrapped 120%: Done")

        assertEquals(TorServiceRuntimeState.RUNNING, parsed.state)
        assertEquals(100, parsed.bootstrapPercent)
        assertEquals("Bootstrapped 120%: Done", parsed.message)
    }

    @Test
    fun parseErrorMessage_overridesStatusSignal() {
        val parsed = parser.parseStatus(
            status = "ON",
            errorMessage = "control socket unavailable"
        )

        assertEquals(TorServiceRuntimeState.ERROR, parsed.state)
        assertEquals(0, parsed.bootstrapPercent)
        assertEquals("control socket unavailable", parsed.message)
    }

    @Test
    fun parseNullStatus_withoutErrorReturnsUnknown() {
        val parsed = parser.parseStatus(status = null)

        assertEquals(TorServiceRuntimeState.UNKNOWN, parsed.state)
        assertEquals(0, parsed.bootstrapPercent)
        assertEquals("", parsed.message)
    }
}
