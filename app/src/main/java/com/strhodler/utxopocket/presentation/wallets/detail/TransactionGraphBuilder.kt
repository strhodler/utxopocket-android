package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import kotlin.math.absoluteValue

private const val HUB_NODE_ID = "tx-hub"
private const val INPUT_GROUP_ID = "inputs-group"
private const val OUTPUT_GROUP_ID = "outputs-group"
private const val FEE_NODE_ID = "fee"

data class GraphNode(
    val id: String,
    val role: GraphRole,
    val valueSats: Long?,
    val address: String?,
    val isMine: Boolean,
    val derivationPath: String?,
    val children: Int = 0
)

data class GraphEdge(val from: String, val to: String)

data class GraphGroup(
    val id: String,
    val members: List<GraphNode>,
    val edges: List<GraphEdge>
)

enum class GraphRole { Input, Output, Change, Fee, Group }

data class GraphSummary(
    val inputCount: Int,
    val outputCount: Int,
    val virtualSize: Long?,
    val feeRateSatPerVb: Double?,
    val feeSats: Long?
)

data class TransactionGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val groups: Map<String, GraphGroup>,
    val summary: GraphSummary,
    val seed: Long
)

fun buildTransactionGraph(transaction: WalletTransaction): TransactionGraph {
    val groups = mutableMapOf<String, GraphGroup>()
    val nodes = mutableListOf<GraphNode>()
    val edges = mutableListOf<GraphEdge>()
    val seed = transaction.id.fold(0L) { acc, char -> acc * 31 + char.code }
    val hubNode = GraphNode(
        id = HUB_NODE_ID,
        role = GraphRole.Group,
        valueSats = null,
        address = null,
        isMine = true,
        derivationPath = null,
        children = transaction.inputs.size + transaction.outputs.size
    )
    nodes += hubNode

    val inputNodes = transaction.inputs.mapIndexed { index, input ->
        GraphNode(
            id = "vin-$index",
            role = GraphRole.Input,
            valueSats = input.valueSats,
            address = input.address ?: formatOutPoint(input.prevTxid, input.prevVout),
            isMine = input.isMine,
            derivationPath = input.derivationPath
        )
    }
    if (inputNodes.size > 6) {
        val groupNode = GraphNode(
            id = INPUT_GROUP_ID,
            role = GraphRole.Group,
            valueSats = null,
            address = null,
            isMine = false,
            derivationPath = null,
            children = inputNodes.size
        )
        nodes += groupNode
        edges += GraphEdge(from = groupNode.id, to = hubNode.id)
        groups[groupNode.id] = GraphGroup(
            id = groupNode.id,
            members = inputNodes,
            edges = inputNodes.map { node ->
                GraphEdge(from = node.id, to = hubNode.id)
            }
        )
    } else {
        nodes += inputNodes
        edges += inputNodes.map { node -> GraphEdge(from = node.id, to = hubNode.id) }
    }

    val outputNodes = transaction.outputs.map { output ->
        val role = if (output.addressType == WalletAddressType.CHANGE) {
            GraphRole.Change
        } else {
            GraphRole.Output
        }
        GraphNode(
            id = "vout-${output.index}",
            role = role,
            valueSats = output.valueSats,
            address = output.address,
            isMine = output.isMine,
            derivationPath = output.derivationPath
        )
    }
    val changeNodes = outputNodes.filter { it.role == GraphRole.Change }
    val externalOutputs = outputNodes.filter { it.role != GraphRole.Change }

    if (externalOutputs.size > 6) {
        val groupNode = GraphNode(
            id = OUTPUT_GROUP_ID,
            role = GraphRole.Group,
            valueSats = null,
            address = null,
            isMine = false,
            derivationPath = null,
            children = externalOutputs.size
        )
        nodes += changeNodes
        nodes += groupNode
        edges += changeNodes.map { node -> GraphEdge(from = hubNode.id, to = node.id) }
        edges += GraphEdge(from = hubNode.id, to = groupNode.id)
        groups[groupNode.id] = GraphGroup(
            id = groupNode.id,
            members = externalOutputs,
            edges = externalOutputs.map { node ->
                GraphEdge(from = hubNode.id, to = node.id)
            }
        )
    } else {
        nodes += outputNodes
        edges += outputNodes.map { node -> GraphEdge(from = hubNode.id, to = node.id) }
    }

    transaction.feeSats?.let { feeSats ->
        val feeNode = GraphNode(
            id = FEE_NODE_ID,
            role = GraphRole.Fee,
            valueSats = feeSats,
            address = null,
            isMine = true,
            derivationPath = null
        )
        nodes += feeNode
        edges += GraphEdge(from = hubNode.id, to = feeNode.id)
    }

    val summary = GraphSummary(
        inputCount = transaction.inputs.size,
        outputCount = transaction.outputs.size,
        virtualSize = transaction.virtualSize,
        feeRateSatPerVb = transaction.feeRateSatPerVb,
        feeSats = transaction.feeSats
    )

    return TransactionGraph(
        nodes = nodes,
        edges = edges,
        groups = groups,
        summary = summary,
        seed = seed.absoluteValue
    )
}

private fun formatOutPoint(txid: String, vout: Int): String {
    val trimmed = if (txid.length <= 12) txid else "${txid.take(8)}...${txid.takeLast(4)}"
    return "$trimmed:$vout"
}
