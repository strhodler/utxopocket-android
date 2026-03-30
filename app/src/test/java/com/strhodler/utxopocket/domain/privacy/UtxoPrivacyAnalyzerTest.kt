package com.strhodler.utxopocket.domain.privacy

import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletUtxo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtxoPrivacyAnalyzerTest {

    private val analyzer = UtxoPrivacyAnalyzer(
        crossHeuristicRules = CrossHeuristicRules()
    )

    @Test
    fun analyzeFlagsReusedReceiveAddressExposure() {
        val findings = analyzer.analyze(
            utxo = utxo(addressReuseCount = 3, addressType = WalletAddressType.EXTERNAL),
            relatedTransactionLabel = null,
            assignedCollection = null
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.UTXO_ADDRESS_REUSE })
    }

    @Test
    fun analyzeFlagsDustOrNearDustExposure() {
        val findings = analyzer.analyze(
            utxo = utxo(valueSats = 700),
            relatedTransactionLabel = null,
            assignedCollection = null
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.UTXO_DUST_WARNING })
    }

    @Test
    fun analyzeFlagsChangeOriginContext() {
        val findings = analyzer.analyze(
            utxo = utxo(addressType = WalletAddressType.CHANGE),
            relatedTransactionLabel = null,
            assignedCollection = null
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.UTXO_CHANGE_ORIGIN })
    }

    @Test
    fun analyzeFlagsOrganizationGapWhenUnlabeledAndUnassigned() {
        val findings = analyzer.analyze(
            utxo = utxo(label = null, transactionLabel = null),
            relatedTransactionLabel = null,
            assignedCollection = null
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.UTXO_ORGANIZATION_GAP })
    }

    @Test
    fun analyzeFlagsSpendabilityOrMaturityContext() {
        val findings = analyzer.analyze(
            utxo = utxo(spendable = false, confirmations = 0),
            relatedTransactionLabel = null,
            assignedCollection = null
        )

        assertTrue(findings.any { it.id == PrivacyFindingIds.UTXO_SPENDABILITY_CONTEXT })
    }

    @Test
    fun analyzeSkipsOrganizationGapWhenCollectionContextExists() {
        val findings = analyzer.analyze(
            utxo = utxo(label = null, transactionLabel = null),
            relatedTransactionLabel = null,
            assignedCollection = collection(name = "Long term")
        )

        assertFalse(findings.any { it.id == PrivacyFindingIds.UTXO_ORGANIZATION_GAP })
    }

    private fun utxo(
        txid: String = "txid",
        vout: Int = 0,
        valueSats: Long = 50_000,
        confirmations: Int = 6,
        label: String? = "Income",
        transactionLabel: String? = null,
        spendable: Boolean = true,
        addressType: WalletAddressType? = WalletAddressType.EXTERNAL,
        addressReuseCount: Int = 1,
        derivationPath: String? = null
    ): WalletUtxo = WalletUtxo(
        txid = txid,
        vout = vout,
        valueSats = valueSats,
        confirmations = confirmations,
        label = label,
        transactionLabel = transactionLabel,
        spendable = spendable,
        addressType = addressType,
        addressReuseCount = addressReuseCount,
        derivationPath = derivationPath
    )

    private fun collection(name: String): UtxoCollection = UtxoCollection(
        id = 10L,
        walletId = 1L,
        name = name,
        color = UtxoCollectionColor.Mint,
        createdAt = 0L,
        updatedAt = 0L
    )
}
