package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionGraphBuilderTest {

    @Test
    fun `groups inputs when above threshold`() {
        val transaction = buildTransaction(inputCount = 7, outputCount = 1)

        val graph = buildTransactionGraph(transaction)

        val inputGroup = graph.nodes.firstOrNull { it.id == "inputs-group" }
        assertNotNull("Expected inputs to be grouped", inputGroup)
        assertEquals(7, inputGroup?.children)
        assertTrue(graph.groups.containsKey("inputs-group"))
        assertTrue(graph.edges.contains(GraphEdge(from = "inputs-group", to = "tx-hub")))
    }

    @Test
    fun `keeps change output visible while grouping externals`() {
        val transaction = buildTransaction(
            inputCount = 2,
            outputCount = 8,
            changeIndices = setOf(0)
        )

        val graph = buildTransactionGraph(transaction)

        val changeNode = graph.nodes.firstOrNull { it.id == "vout-0" }
        assertEquals(GraphRole.Change, changeNode?.role)
        val outputGroup = graph.nodes.firstOrNull { it.id == "outputs-group" }
        assertNotNull("Expected external outputs to be grouped", outputGroup)
        assertEquals(7, outputGroup?.children)
        assertTrue(graph.edges.contains(GraphEdge(from = "tx-hub", to = "outputs-group")))
    }

    @Test
    fun `includes fee node when available`() {
        val transaction = buildTransaction(
            inputCount = 1,
            outputCount = 1,
            feeSats = 500L
        )

        val graph = buildTransactionGraph(transaction)

        val feeNode = graph.nodes.firstOrNull { it.id == "fee" }
        assertNotNull(feeNode)
        assertEquals(GraphRole.Fee, feeNode?.role)
        assertTrue(graph.edges.contains(GraphEdge(from = "tx-hub", to = "fee")))
    }

    private fun buildTransaction(
        inputCount: Int,
        outputCount: Int,
        feeSats: Long? = null,
        changeIndices: Set<Int> = emptySet()
    ): WalletTransaction {
        val inputs = (0 until inputCount).map { index ->
            WalletTransactionInput(
                prevTxid = "prev-$index",
                prevVout = index,
                valueSats = 1_000L + index,
                address = "input-$index",
                isMine = index % 2 == 0,
                addressType = WalletAddressType.EXTERNAL
            )
        }
        val outputs = (0 until outputCount).map { index ->
            val isChange = changeIndices.contains(index)
            WalletTransactionOutput(
                index = index,
                valueSats = 2_000L + index,
                address = "output-$index",
                isMine = isChange,
                addressType = if (isChange) WalletAddressType.CHANGE else WalletAddressType.EXTERNAL,
                derivationPath = if (isChange) "m/1/$index" else null
            )
        }
        return WalletTransaction(
            id = "tx-test",
            amountSats = 0,
            timestamp = 0L,
            type = TransactionType.RECEIVED,
            confirmations = 0,
            feeSats = feeSats,
            inputs = inputs,
            outputs = outputs
        )
    }
}
