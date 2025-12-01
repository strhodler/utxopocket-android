package com.strhodler.utxopocket.presentation.wallets.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Paint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.presentation.format.formatBtc
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.joints.MouseJoint
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
    SetSecondaryTopBar(
        title = stringResource(id = R.string.transaction_visualizer_title),
        onBackClick = onBack
    )
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
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
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionVisualizerContent(
    state: TransactionVisualizerUiState,
    onBack: () -> Unit
) {
    val graph = requireNotNull(state.graph)
    var renderGraph by remember(graph) { mutableStateOf(graph) }
    var showDetails by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp) {
        configuration.screenHeightDp.dp * 0.45f
    }
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
        confirmValueChange = { value -> value != SheetValue.PartiallyExpanded }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val coroutineScope = rememberCoroutineScope()
    val sheetVisible = scaffoldState.bottomSheetState.currentValue != SheetValue.Hidden
    LaunchedEffect(renderGraph) {
        val currentSelection = selectedNodeId
        if (currentSelection != null && renderGraph.nodes.none { it.id == currentSelection }) {
            selectedNodeId = null
        }
    }
    LaunchedEffect(selectedNodeId) {
        if (selectedNodeId == null) {
            showDetails = false
        }
    }

    LaunchedEffect(showDetails, selectedNodeId) {
        if (showDetails && selectedNodeId != null) {
            coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
        } else {
            coroutineScope.launch { scaffoldState.bottomSheetState.hide() }
        }
    }
    LaunchedEffect(sheetVisible) {
        if (!sheetVisible) {
            showDetails = false
            selectedNodeId = null
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetSwipeEnabled = selectedNodeId != null,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            val transaction = requireNotNull(state.transaction)
            val selectedNode = selectedNodeId?.let { id ->
                renderGraph.nodes.firstOrNull { it.id == id }
            }
            when {
                selectedNode == null -> {
                    Spacer(modifier = Modifier.height(1.dp))
                }
                selectedNode.id == TRANSACTION_HUB_ID -> {
                    TransactionInfoSheet(
                        transaction = transaction,
                        inputCount = renderGraph.summary.inputCount,
                        outputCount = renderGraph.summary.outputCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight)
                            .navigationBarsPadding()
                    )
                }
                selectedNode.role == GraphRole.Fee -> {
                    FeeInfoSheet(
                        transaction = transaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight)
                            .navigationBarsPadding()
                    )
                }
                else -> {
                    NodeInfoSheet(
                        node = selectedNode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val canvasBottomPadding by animateDpAsState(
                targetValue = if (sheetVisible) maxSheetHeight else 0.dp,
                animationSpec = tween(durationMillis = 180),
                label = "canvasBottomPadding"
            )
            Box(modifier = Modifier.fillMaxSize()) {
            TransactionGraphCanvas(
                graph = renderGraph,
                selectedNodeId = selectedNodeId,
                onNodeSelected = { node, fromTap ->
                    selectedNodeId = node?.id
                    if (fromTap && node != null) {
                        showDetails = true
                    } else if (node == null) {
                        showDetails = false
                    }
                },
                onGroupExpand = { groupId ->
                    renderGraph = expandGroup(renderGraph, groupId)
                },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = canvasBottomPadding)
                        .clipToBounds()
                )
            }
        }
    }
}

@Composable
private fun TransactionGraphCanvas(
    graph: TransactionGraph,
    selectedNodeId: String?,
    onNodeSelected: (GraphNode?, Boolean) -> Unit,
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
        physicsEngine?.let { engine ->
            mouseJoint?.let { joint ->
                runCatching { engine.world.destroyJoint(joint) }
            }
        }
        mouseJoint = null
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
                        onNodeSelected(tapped.node, false)
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
                                    onNodeSelected(tapped.node, false)
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
                        onNodeSelected(tapped.node, true)
                    } else {
                        onNodeSelected(null, true)
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
        GraphRole.Group -> 26.dp
        GraphRole.Input, GraphRole.Output -> 22.dp
    }
    val scaled = if (node.children > 0) base * 1.1f else base
    return with(density) { scaled.toPx() }
}

private fun nodeColor(node: GraphNode, colors: ColorScheme): Color =
    when (node.role) {
        GraphRole.Change -> colors.secondaryContainer
        GraphRole.Fee -> colors.errorContainer
        GraphRole.Group -> colors.surfaceContainerHigh
        GraphRole.Input -> if (node.isMine) colors.primaryContainer else colors.surfaceVariant
        GraphRole.Output -> if (node.isMine) colors.primaryContainer else colors.surfaceVariant
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

@Composable
private fun NodeInfoSheet(
    node: GraphNode,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val ownershipLabel = when (node.role) {
        GraphRole.Input, GraphRole.Output, GraphRole.Change -> if (node.isMine) {
            stringResource(id = R.string.transaction_visualizer_wallet_owned)
        } else {
            stringResource(id = R.string.transaction_visualizer_external_owned)
        }
        else -> null
    }
    val valueLabel = node.valueSats?.let { formatBtc(it) }
        ?: stringResource(id = R.string.transaction_detail_unknown)
    val addressLabel = node.address?.let { address ->
        formatTxidMiddle(address, keepStart = 12, keepEnd = 8)
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(colorScheme.surface)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color = nodeColor(node, colorScheme), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = roleLabel(node)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ownershipLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_visualizer_value_title),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        )
        addressLabel?.let { formatted ->
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.transaction_visualizer_address_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
        node.derivationPath?.let { path ->
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.transaction_visualizer_derivation_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun TransactionInfoSheet(
    transaction: WalletTransaction,
    inputCount: Int,
    outputCount: Int,
    modifier: Modifier = Modifier
) {
    val copyToClipboard = rememberCopyToClipboard(
        successMessage = stringResource(id = R.string.transaction_detail_copy_id)
    )
    val dateLabel = transaction.timestamp?.let { timestamp ->
        remember(timestamp) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val confirmationsLabel = if (transaction.confirmations >= 0) {
        transaction.confirmations.toString()
    } else {
        stringResource(id = R.string.transaction_detail_unknown)
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_detail_id_label),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = transaction.id,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                IconButton(onClick = { copyToClipboard(transaction.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(id = R.string.transaction_detail_copy_id)
                    )
                }
            }
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_detail_time),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_detail_confirmations_label),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = confirmationsLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_visualizer_summary_inputs, inputCount),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = inputCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_visualizer_summary_outputs, outputCount),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = outputCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
private fun FeeInfoSheet(
    transaction: WalletTransaction,
    modifier: Modifier = Modifier
) {
    val feeLabel = transaction.feeSats?.let { sats ->
        "$sats sats"
    } ?: stringResource(id = R.string.transaction_detail_unknown)
    val feeRateLabel = transaction.feeRateSatPerVb?.let { rate ->
        String.format(Locale.getDefault(), "%.2f sats/vB", rate)
    } ?: stringResource(id = R.string.transaction_detail_unknown)
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_detail_fee),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            trailingContent = {
                Text(
                    text = feeLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.transaction_detail_fee_rate),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            trailingContent = {
                Text(
                    text = feeRateLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
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
