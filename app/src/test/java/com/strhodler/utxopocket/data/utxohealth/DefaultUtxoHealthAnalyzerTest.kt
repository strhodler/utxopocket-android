package com.strhodler.utxopocket.data.utxohealth

import com.strhodler.utxopocket.domain.model.UtxoAnalysisContext
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthIndicatorType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.UtxoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultUtxoHealthAnalyzerTest {

    private val analyzer = DefaultUtxoHealthAnalyzer()

    private val defaultContext = UtxoAnalysisContext(
        dustThresholdUser = 600L,
        parameters = UtxoHealthParameters()
    )

    @Test
    fun reusePenaltyActivatesWhenCountAboveThreshold() {
        val utxo = baseUtxo.copy(
            addressReuseCount = 4,
            address = "bc1qreuse"
        )

        val result = analyzer.analyze(utxo, defaultContext)

        assertTrue(result.indicators.any { it.type == UtxoHealthIndicatorType.ADDRESS_REUSE })
        assertTrue(result.badges.any { it.id == "address_reuse" })
        assertTrue(result.finalScore < 100)
    }

    @Test
    fun dustPenaltyWhenValueBelowThreshold() {
        val utxo = baseUtxo.copy(valueSats = 400L)

        val result = analyzer.analyze(utxo, defaultContext)

        assertTrue(result.indicators.any { it.type == UtxoHealthIndicatorType.DUST_UTXO })
        assertTrue(result.badges.any { it.id == "dust_utxo" })
    }

    @Test
    fun changeUnconsolidatedTriggersAfterConfirmations() {
        val utxo = baseUtxo.copy(
            addressType = WalletAddressType.CHANGE,
            confirmations = 24,
            valueSats = 50_000L
        )

        val result = analyzer.analyze(utxo, defaultContext)

        assertTrue(result.indicators.any { it.type == UtxoHealthIndicatorType.CHANGE_UNCONSOLIDATED })
    }

    @Test
    fun missingLabelOnHighValueOutputs() {
        val utxo = baseUtxo.copy(
            valueSats = 1_000_000L,
            label = null
        )

        val result = analyzer.analyze(utxo, defaultContext)

        assertTrue(result.indicators.any { it.type == UtxoHealthIndicatorType.MISSING_LABEL })
    }

    @Test
    fun healthyUtxoKeepsHighScore() {
        val utxo = baseUtxo.copy(
            valueSats = 700_000L,
            label = "cold storage",
            confirmations = 6,
            addressReuseCount = 1
        )

        val result = analyzer.analyze(utxo, defaultContext)

        assertEquals(100, result.finalScore)
        assertTrue(result.badges.any { it.id == "utxo_healthy" })
    }

    private val baseUtxo = WalletUtxo(
        txid = "txid",
        vout = 0,
        valueSats = 50_000L,
        confirmations = 1,
        label = "label",
        status = UtxoStatus.CONFIRMED,
        address = "bc1qaddress",
        addressType = WalletAddressType.EXTERNAL,
        derivationIndex = 0,
        derivationPath = "0/0",
        addressReuseCount = 1
    )
}
