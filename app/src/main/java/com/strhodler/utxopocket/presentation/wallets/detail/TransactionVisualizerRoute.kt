package com.strhodler.utxopocket.presentation.wallets.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.presentation.format.formatBtc
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.navigation.SetHiddenTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.joints.MouseJoint

@Composable
fun TransactionVisualizerRoute(
    onBack: () -> Unit,
    viewModel: TransactionVisualizerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionVisualizerScreen(
        state = state,
        onBack = onBack
    )
}

@HiltViewModel
class TransactionVisualizerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    walletRepository: WalletRepository,
    appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val transactionId: String =
        savedStateHandle.get<String>(WalletsNavigation.TransactionIdArg)
            ?: error("Transaction id is required")

    val uiState: StateFlow<TransactionVisualizerUiState> = combine(
        walletRepository.observeWalletDetail(walletId),
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden
    ) { detail, balanceUnit, balancesHidden ->
        val transaction = detail?.transactions?.firstOrNull { it.id == transactionId }
        when {
            detail == null -> TransactionVisualizerUiState(
                isLoading = false,
                walletSummary = null,
                transaction = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                graph = null,
                hasInsufficientData = false,
                error = TransactionVisualizerError.NotFound
            )

            transaction == null -> TransactionVisualizerUiState(
                isLoading = false,
                walletSummary = detail.summary,
                transaction = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                graph = null,
                hasInsufficientData = false,
                error = TransactionVisualizerError.NotFound
            )

            else -> {
                val graph = buildTransactionGraph(transaction)
                val hasInsufficientData =
                    transaction.rawHex == null || transaction.inputs.isEmpty() || transaction.outputs.isEmpty()
                TransactionVisualizerUiState(
                    isLoading = false,
                    walletSummary = detail.summary,
                    transaction = transaction,
                    balanceUnit = balanceUnit,
                    balancesHidden = balancesHidden,
                    graph = graph,
                    hasInsufficientData = hasInsufficientData,
                    error = null
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionVisualizerUiState()
    )
}

data class TransactionVisualizerUiState(
    val isLoading: Boolean = true,
    val walletSummary: WalletSummary? = null,
    val transaction: WalletTransaction? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false,
    val graph: TransactionGraph? = null,
    val hasInsufficientData: Boolean = false,
    val error: TransactionVisualizerError? = null
)

sealed interface TransactionVisualizerError {
    data object NotFound : TransactionVisualizerError
}

@Composable
private fun TransactionVisualizerScreen(
    state: TransactionVisualizerUiState,
    onBack: () -> Unit
) {
    SetHiddenTopBar()
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    when {
        state.isLoading -> VisualizerLoader(modifier = Modifier.fillMaxSize())
        state.error != null || state.transaction == null || state.graph == null -> VisualizerError(
            onBack = onBack,
            modifier = Modifier.fillMaxSize()
        )

        else -> TransactionVisualizerContent(
            state = state,
            onBack = onBack
        )
    }
}

@Composable
private fun TransactionVisualizerContent(
    state: TransactionVisualizerUiState,
    onBack: () -> Unit
) {
    val graph = requireNotNull(state.graph)
    var renderGraph by remember(graph) { mutableStateOf(graph) }
    var showDetails by remember { mutableStateOf(false) }
    var panelIconIsClose by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(renderGraph) {
        val currentSelection = selectedNodeId
        if (currentSelection != null && renderGraph.nodes.none { it.id == currentSelection }) {
            selectedNodeId = null
        }
    }
    LaunchedEffect(showDetails) {
        if (showDetails) {
            panelIconIsClose = true
        } else {
            delay(200L)
            panelIconIsClose = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TransactionGraphCanvas(
                        graph = renderGraph,
                        selectedNodeId = selectedNodeId,
                        onNodeSelected = { selectedNodeId = it?.id },
                        onGroupExpand = { groupId ->
                            renderGraph = expandGroup(renderGraph, groupId)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(id = R.string.transaction_visualizer_back)
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showDetails,
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 160)) { fullWidth -> fullWidth },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 160)) { fullWidth -> fullWidth },
                    modifier = Modifier.fillMaxHeight()
                ) {
                TransactionDetailsPanel(
                    transaction = requireNotNull(state.transaction),
                    graph = renderGraph,
                    selectedNodeId = selectedNodeId,
                    onNodeSelected = { selectedNodeId = it?.id },
                    onGroupExpand = { groupId ->
                        renderGraph = expandGroup(renderGraph, groupId)
                    },
                    onGroupMemberSelect = { groupId, member ->
                        renderGraph = extractNodeFromGroup(renderGraph, groupId, member.id)
                        selectedNodeId = member.id
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                )
            }
            }
            IconButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (panelIconIsClose) Icons.Outlined.Close else Icons.Outlined.Menu,
                    contentDescription = stringResource(id = R.string.transaction_visualizer_toggle_details)
                )
            }
        }
    }
}

@Composable
private fun TransactionGraphCanvas(
    graph: TransactionGraph,
    selectedNodeId: String?,
    onNodeSelected: (GraphNode?) -> Unit,
    onGroupExpand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val composeDensity = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var nodeLayouts by remember(graph) { mutableStateOf<List<NodeLayout>>(emptyList()) }
    var physicsEngine by remember(graph) { mutableStateOf<PhysicsEngine?>(null) }
    var mouseJoint by remember { mutableStateOf<MouseJoint?>(null) }
    val colorScheme = MaterialTheme.colorScheme
    val textPaint = rememberTextPaint(colorScheme.onSurface)
    val nodeLayoutsState = rememberUpdatedState(nodeLayouts)
    val graphState = rememberUpdatedState(graph)

    LaunchedEffect(graph, canvasSize, composeDensity) {
        if (canvasSize.width == 0 || canvasSize.height == 0) return@LaunchedEffect
        physicsEngine = buildPhysicsEngine(graph, canvasSize, composeDensity)
        nodeLayouts = physicsEngine?.layouts().orEmpty()
    }

    LaunchedEffect(physicsEngine) {
        val engine = physicsEngine ?: return@LaunchedEffect
        while (isActive) {
            engine.world.step(1f / 60f, 8, 3)
            nodeLayouts = engine.layouts()
            delay(16L)
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(graph, physicsEngine) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val tapped = nodeLayoutsState.value.findLast { it.contains(offset) } ?: return@detectDragGestures
                        val engine = physicsEngine ?: return@detectDragGestures
                        val currentGraph = graphState.value
                        if (currentGraph.groups.containsKey(tapped.node.id)) {
                            onGroupExpand(tapped.node.id)
                            return@detectDragGestures
                        }
                        onNodeSelected(tapped.node)
                        mouseJoint?.let { engine.world.destroyJoint(it) }
                        mouseJoint = engine.createMouseJoint(tapped.node.id, offset)
                        mouseJoint?.bodyB?.isAwake = true
                    },
                    onDrag = { change, _ ->
                        val engine = physicsEngine ?: return@detectDragGestures
                        mouseJoint?.let { joint ->
                            change.consume()
                            joint.setTarget(
                                Vec2(
                                    change.position.x / engine.pixelsPerMeter,
                                    change.position.y / engine.pixelsPerMeter
                                )
                            )
                            joint.bodyB?.isAwake = true
                        } ?: run {
                            val tapped = nodeLayoutsState.value.findLast { it.contains(change.position) }
                            val currentGraph = graphState.value
                            if (tapped != null) {
                                if (currentGraph.groups.containsKey(tapped.node.id)) {
                                    onGroupExpand(tapped.node.id)
                                } else {
                                    onNodeSelected(tapped.node)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        val engine = physicsEngine
                        mouseJoint?.let { joint -> engine?.world?.destroyJoint(joint) }
                        mouseJoint = null
                    },
                    onDragCancel = {
                        val engine = physicsEngine
                        mouseJoint?.let { joint -> engine?.world?.destroyJoint(joint) }
                        mouseJoint = null
                    }
                )
            }
            .pointerInput(graph) {
                detectTapGestures { offset ->
                    val tapped = nodeLayoutsState.value.findLast { it.contains(offset) }
                    val currentGraph = graphState.value
                    if (tapped != null && currentGraph.groups.containsKey(tapped.node.id)) {
                        onGroupExpand(tapped.node.id)
                    } else if (tapped != null) {
                        onNodeSelected(tapped.node)
                    } else {
                        onNodeSelected(null)
                    }
                }
            }
    ) {
        val layoutById = nodeLayouts.associateBy { it.node.id }
        graph.edges.forEach { edge ->
            val from = layoutById[edge.from] ?: return@forEach
            val to = layoutById[edge.to] ?: return@forEach
            val edgeColor = if (selectedNodeId == null ||
                selectedNodeId == from.node.id ||
                selectedNodeId == to.node.id
            ) {
                colorScheme.outline
            } else {
                colorScheme.outlineVariant
            }
            drawLine(
                color = edgeColor,
                start = from.center,
                end = to.center,
                strokeWidth = 2.dp.toPx()
            )
        }
        nodeLayouts.forEach { state ->
            val isSelected = selectedNodeId == null || selectedNodeId == state.node.id
            val fillColor = if (isSelected) nodeColor(state.node, colorScheme) else colorScheme.surfaceVariant
            val strokeColor = if (isSelected) colorScheme.outline else colorScheme.outlineVariant
            val textColor = if (isSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
            val stroke = Stroke(width = 1.dp.toPx())
            drawCircle(
                color = colorScheme.surfaceContainerHighest,
                radius = state.radiusPx + 6.dp.toPx(),
                center = state.center
            )
            drawCircle(
                color = fillColor,
                radius = state.radiusPx,
                center = state.center
            )
            drawCircle(
                color = strokeColor,
                radius = state.radiusPx,
                center = state.center,
                style = stroke
            )
            drawIntoCanvas { canvas ->
                val label = nodeLabel(state.node)
                val previousColor = textPaint.color
                textPaint.color = textColor.toArgb()
                canvas.nativeCanvas.drawText(
                    label,
                    state.center.x,
                    state.center.y + textPaint.textSize / 3,
                    textPaint
                )
                textPaint.color = previousColor
            }
        }
    }
}

@Composable
private fun VisualizerLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun VisualizerError(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.transaction_visualizer_not_found),
            style = MaterialTheme.typography.bodyLarge
        )
        TextButton(onClick = onBack) {
            Text(text = stringResource(id = R.string.transaction_visualizer_back))
        }
    }
}

private const val TRANSACTION_HUB_ID = "tx-hub"
private const val GROUP_CHUNK_SIZE = 50

internal fun radiusFor(node: GraphNode, density: Density): Float {
    val base = when (node.role) {
        GraphRole.Change -> 26.dp
        GraphRole.Fee -> 18.dp
        GraphRole.Group -> 30.dp
        GraphRole.Input, GraphRole.Output -> 22.dp
    }
    val scaled = if (node.children > 0) base * 1.1f else base
    return with(density) { scaled.toPx() }
}

private fun nodeColor(node: GraphNode, colors: ColorScheme): Color =
    when (node.role) {
        GraphRole.Change -> colors.secondary
        GraphRole.Fee -> colors.surfaceVariant
        GraphRole.Group -> colors.surfaceVariant
        GraphRole.Input,
        GraphRole.Output -> if (node.isMine) colors.primary else colors.tertiary
    }

private fun expandGroup(graph: TransactionGraph, groupId: String): TransactionGraph {
    val group = graph.groups[groupId] ?: return graph
    if (group.members.size > GROUP_CHUNK_SIZE) {
        return chunkGroup(graph, groupId, GROUP_CHUNK_SIZE)
    }
    val updatedNodes = graph.nodes.filterNot { it.id == groupId } + group.members
    val updatedEdges = graph.edges.filterNot { it.from == groupId || it.to == groupId } + group.edges
    val updatedGroups = graph.groups - groupId
    return graph.copy(nodes = updatedNodes, edges = updatedEdges, groups = updatedGroups)
}

private fun chunkGroup(
    graph: TransactionGraph,
    groupId: String,
    chunkSize: Int
): TransactionGraph {
    val group = graph.groups[groupId] ?: return graph
    val baseNode = graph.nodes.firstOrNull { it.id == groupId } ?: return graph
    val existingEdges = graph.edges.filterNot { it.from == groupId || it.to == groupId }.toMutableList()
    val updatedGroups = graph.groups.toMutableMap()
    updatedGroups.remove(groupId)
    val hubEdgeSample = group.edges.firstOrNull()
    val connectsFromHub = hubEdgeSample?.from == TRANSACTION_HUB_ID
    val connectsToHub = hubEdgeSample?.to == TRANSACTION_HUB_ID
    val newGroups = mutableListOf<GraphGroup>()
    val newNodes = mutableListOf<GraphNode>()

    group.members.chunked(chunkSize).forEachIndexed { index, members ->
        val newId = "${groupId}-chunk-${index + 1}"
        val memberIds = members.map { it.id }.toSet()
        val memberEdges = group.edges.filter { edge ->
            memberIds.contains(edge.from) || memberIds.contains(edge.to)
        }
        val connector = when {
            connectsFromHub -> GraphEdge(from = TRANSACTION_HUB_ID, to = newId)
            connectsToHub -> GraphEdge(from = newId, to = TRANSACTION_HUB_ID)
            else -> {
                val fromOriginal = graph.edges.firstOrNull { it.from == groupId }
                val toOriginal = graph.edges.firstOrNull { it.to == groupId }
                when {
                    fromOriginal != null -> GraphEdge(from = newId, to = fromOriginal.to)
                    toOriginal != null -> GraphEdge(from = toOriginal.from, to = newId)
                    else -> null
                }
            }
        }
        connector?.let { existingEdges += it }
        val newNode = baseNode.copy(id = newId, children = members.size)
        newNodes += newNode
        newGroups += GraphGroup(
            id = newId,
            members = members,
            edges = memberEdges
        )
    }

    newGroups.forEach { updatedGroups[it.id] = it }
    val updatedNodes = graph.nodes.filterNot { it.id == groupId } + newNodes
    return graph.copy(nodes = updatedNodes, edges = existingEdges.distinct(), groups = updatedGroups)
}

private fun extractNodeFromGroup(
    graph: TransactionGraph,
    groupId: String,
    memberId: String
): TransactionGraph {
    val group = graph.groups[groupId] ?: return graph
    val member = group.members.firstOrNull { it.id == memberId } ?: return graph
    val remainingMembers = group.members.filterNot { it.id == memberId }
    val remainingGroupEdges = group.edges.filterNot { it.from == memberId || it.to == memberId }
    val memberEdges = group.edges.filter { it.from == memberId || it.to == memberId }

    val updatedNodes = graph.nodes.toMutableList()
    if (updatedNodes.none { it.id == member.id }) {
        updatedNodes += member
    }
    val updatedEdges = graph.edges.toMutableList().apply { addAll(memberEdges) }
    val updatedGroups = graph.groups.toMutableMap()

    if (remainingMembers.isEmpty()) {
        updatedNodes.removeAll { it.id == groupId }
        updatedEdges.removeAll { it.from == groupId || it.to == groupId }
        updatedGroups.remove(groupId)
    } else {
        val groupIndex = updatedNodes.indexOfFirst { it.id == groupId }
        if (groupIndex >= 0) {
            val groupNode = updatedNodes[groupIndex]
            updatedNodes[groupIndex] = groupNode.copy(children = remainingMembers.size)
        }
        updatedGroups[groupId] = GraphGroup(
            id = groupId,
            members = remainingMembers,
            edges = remainingGroupEdges
        )
    }

    return graph.copy(
        nodes = updatedNodes,
        edges = updatedEdges.distinct(),
        groups = updatedGroups
    )
}

private fun nodeLabel(node: GraphNode): String = when (node.role) {
    GraphRole.Input -> "IN"
    GraphRole.Output -> "OUT"
    GraphRole.Change -> "CHG"
    GraphRole.Fee -> "FEE"
    GraphRole.Group -> if (node.id == TRANSACTION_HUB_ID) "TX" else node.children.toString()
}

private fun roleLabel(node: GraphNode): Int = when (node.role) {
    GraphRole.Input -> R.string.transaction_visualizer_role_input
    GraphRole.Output -> R.string.transaction_visualizer_role_output
    GraphRole.Change -> R.string.transaction_visualizer_role_change
    GraphRole.Fee -> R.string.transaction_visualizer_role_fee
    GraphRole.Group -> if (node.id == TRANSACTION_HUB_ID) {
        R.string.transaction_visualizer_role_transaction
    } else {
        R.string.transaction_visualizer_role_group
    }
}

private sealed interface PanelItem {
    data class Header(val title: String) : PanelItem
    data class NodeEntry(val node: GraphNode) : PanelItem
    data class GroupEntry(val node: GraphNode) : PanelItem
    data class GroupMember(val groupId: String, val member: GraphNode) : PanelItem
}

@Composable
private fun TransactionDetailsPanel(
    transaction: WalletTransaction,
    graph: TransactionGraph,
    selectedNodeId: String?,
    onNodeSelected: (GraphNode?) -> Unit,
    onGroupExpand: (String) -> Unit,
    onGroupMemberSelect: (String, GraphNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val feeNode = remember(graph) { graph.nodes.firstOrNull { it.role == GraphRole.Fee } }
    val inputNodes = remember(graph) { graph.nodes.filter { it.role == GraphRole.Input } }
    val outputNodes = remember(graph) { graph.nodes.filter { it.role == GraphRole.Output || it.role == GraphRole.Change } }
    val inputGroups = remember(graph) {
        graph.groups.filterValues { group ->
            group.members.firstOrNull()?.role == GraphRole.Input
        }
    }
    val outputGroups = remember(graph) {
        graph.groups.filterValues { group ->
            val role = group.members.firstOrNull()?.role
            role == GraphRole.Output || role == GraphRole.Change
        }
    }
    val listState = rememberLazyListState()
    val items = buildList {
        add(PanelItem.Header(stringResource(id = R.string.transaction_visualizer_summary_inputs, graph.summary.inputCount)))
        inputNodes.sortedBy { it.id }.forEach { add(PanelItem.NodeEntry(it)) }
        inputGroups.toSortedMap().forEach { (groupId, group) ->
            val groupNode = graph.nodes.firstOrNull { it.id == groupId } ?: return@forEach
            add(PanelItem.GroupEntry(groupNode))
            group.members.sortedBy { it.id }.forEach { member ->
                add(PanelItem.GroupMember(groupId, member))
            }
        }
        add(PanelItem.Header(stringResource(id = R.string.transaction_visualizer_summary_outputs, graph.summary.outputCount)))
        outputNodes.sortedBy { it.id }.forEach { add(PanelItem.NodeEntry(it)) }
        outputGroups.toSortedMap().forEach { (groupId, group) ->
            val groupNode = graph.nodes.firstOrNull { it.id == groupId } ?: return@forEach
            add(PanelItem.GroupEntry(groupNode))
            group.members.sortedBy { it.id }.forEach { member ->
                add(PanelItem.GroupMember(groupId, member))
            }
        }
        feeNode?.let { fee ->
            add(PanelItem.Header(stringResource(id = R.string.transaction_visualizer_role_fee)))
            add(PanelItem.NodeEntry(fee))
        }
    }

    LaunchedEffect(selectedNodeId, items) {
        val targetIndex = items.indexOfFirst {
            when (it) {
                is PanelItem.NodeEntry -> it.node.id == selectedNodeId
                is PanelItem.GroupEntry -> it.node.id == selectedNodeId
                is PanelItem.GroupMember -> it.member.id == selectedNodeId
                else -> false
            }
        }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(index = targetIndex, scrollOffset = 0)
        }
    }

    Column(
        modifier = modifier
            .background(colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.transaction_visualizer_details_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTxidMiddle(transaction.id),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight()
        ) {
            itemsIndexed(items, key = { index, item ->
                when (item) {
                    is PanelItem.Header -> "header-$index-${item.title}"
                    is PanelItem.NodeEntry -> "node-${item.node.id}"
                    is PanelItem.GroupEntry -> "group-${item.node.id}"
                    is PanelItem.GroupMember -> "group-${item.groupId}-member-${item.member.id}"
                }
            }) { _, item ->
                when (item) {
                    is PanelItem.Header -> {
                        SectionHeader(
                            text = item.title,
                            color = colorScheme.onSurface
                        )
                    }
                    is PanelItem.NodeEntry -> {
                        TransactionDetailEntry(
                            node = item.node,
                            colorScheme = colorScheme,
                            graph = graph,
                            isSelected = selectedNodeId == item.node.id,
                            onSelect = { onNodeSelected(item.node) }
                        )
                    }
                    is PanelItem.GroupEntry -> {
                        TransactionDetailEntry(
                            node = item.node,
                            colorScheme = colorScheme,
                            graph = graph,
                            isSelected = selectedNodeId == item.node.id,
                            onSelect = { onGroupExpand(item.node.id) }
                        )
                    }
                    is PanelItem.GroupMember -> {
                        TransactionDetailEntry(
                            node = item.member,
                            colorScheme = colorScheme,
                            graph = graph,
                            isSelected = selectedNodeId == item.member.id,
                            isGroupMember = true,
                            onSelect = { onGroupMemberSelect(item.groupId, item.member) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun TransactionDetailEntry(
    node: GraphNode,
    colorScheme: ColorScheme,
    graph: TransactionGraph,
    isSelected: Boolean,
    isGroupMember: Boolean = false,
    onSelect: () -> Unit
) {
    val indicatorColor = nodeColor(node, colorScheme)
    val groupChildren = graph.groups[node.id]?.members?.size ?: node.children
    val indicatorLabel = when (node.role) {
        GraphRole.Input -> stringResource(
            if (node.isMine) R.string.transaction_visualizer_wallet_owned else R.string.transaction_visualizer_role_input
        )
        GraphRole.Output -> stringResource(
            if (node.isMine) R.string.transaction_visualizer_wallet_owned else R.string.transaction_visualizer_legend_external
        )
        GraphRole.Change -> stringResource(id = R.string.transaction_visualizer_role_change)
        GraphRole.Fee -> stringResource(id = R.string.transaction_visualizer_role_fee)
        GraphRole.Group -> stringResource(
            id = R.string.transaction_visualizer_group_child_count,
            groupChildren
        )
    }
    val addressLabel = node.address?.let { address ->
        formatTxidMiddle(address, keepStart = 10, keepEnd = 6)
    }
    val containerColor = if (isSelected) colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
    val supportingColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
    val indentModifier = if (isGroupMember) Modifier.padding(start = 12.dp) else Modifier
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(indentModifier)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = indicatorColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = indicatorLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            node.valueSats?.let { value ->
                Text(
                    text = formatBtc(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = supportingColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        addressLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTxidMiddle(txid: String, keepStart: Int = 10, keepEnd: Int = 6): String {
    if (txid.length <= keepStart + keepEnd + 3) return txid
    val prefix = txid.take(keepStart)
    val suffix = txid.takeLast(keepEnd)
    return "$prefix...$suffix"
}

@Composable
private fun rememberTextPaint(color: Color): Paint {
    val density = LocalDensity.current
    return remember(color, density) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 12.sp.toPx() }
            this.color = color.toArgb()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
