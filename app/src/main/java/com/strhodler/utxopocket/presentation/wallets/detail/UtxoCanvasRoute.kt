package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemRef
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemType
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.abbreviatedBalanceText
import com.strhodler.utxopocket.presentation.components.UtxoIdenticon
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import android.os.SystemClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UtxoCanvasRoute(
    onBack: () -> Unit,
    onOpenUtxo: (String, Int) -> Unit,
    onOpenCollection: (Long) -> Unit,
    viewModel: UtxoCanvasViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetSecondaryTopBar(
        title = stringResource(id = R.string.wallet_utxo_canvas_title),
        onBackClick = onBack
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { contentPadding ->
        when {
            state.summary == null && state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.summary == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_not_found),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                UtxoCanvasScreen(
                    state = state,
                    onOpenUtxo = onOpenUtxo,
                    onOpenCollection = onOpenCollection,
                    onReorder = viewModel::updateCanvasOrder,
                    onCreateCollection = viewModel::createCollection,
                    onAddToCollection = viewModel::addUtxoToCollection,
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UtxoCanvasScreen(
    state: UtxoCanvasUiState,
    onOpenUtxo: (String, Int) -> Unit,
    onOpenCollection: (Long) -> Unit,
    onReorder: (List<UtxoCanvasItemUi>) -> Unit,
    onCreateCollection: (String, UtxoCollectionColor, List<UtxoCanvasItemUi.Utxo>, Int?) -> Unit,
    onAddToCollection: (UtxoCanvasItemUi.Utxo, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = state.items
    var pendingDraft by remember { mutableStateOf<CollectionDraft?>(null) }
    var collectionName by remember { mutableStateOf("") }
    var collectionNameError by remember { mutableStateOf<String?>(null) }
    var collectionColor by remember { mutableStateOf(UtxoCollectionColor.Mint) }
    val blankNameError = stringResource(id = R.string.wallet_utxo_canvas_collection_name_error_blank)
    val duplicateNameError = stringResource(id = R.string.wallet_utxo_canvas_collection_name_error_exists)
    val availableColors = remember(state.collections) {
        availableCollectionColors(state.collections)
    }
    val hasAvailableColors = availableColors.isNotEmpty()
    val nextColor = remember(state.collections) {
        nextCollectionColor(state.collections)
    }

    LaunchedEffect(pendingDraft) {
        if (pendingDraft == null) {
            collectionName = ""
            collectionNameError = null
        } else {
            if (hasAvailableColors) {
                collectionColor = nextColor
            }
        }
    }
    LaunchedEffect(availableColors) {
        if (availableColors.isNotEmpty() && collectionColor !in availableColors) {
            collectionColor = availableColors.first()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.isEmpty()) {
            EmptyCanvasState()
        } else {
            UtxoCanvasGrid(
                items = items,
                balanceUnit = state.balanceUnit,
                balancesHidden = state.balancesHidden,
                onUtxoClick = { utxo -> onOpenUtxo(utxo.txid, utxo.vout) },
                onCollectionClick = { collection -> onOpenCollection(collection.id) },
                onReorder = onReorder,
                onDropOnCollection = { utxo, collectionId ->
                    onAddToCollection(utxo, collectionId)
                },
                onRequestCollection = { dragged, target, anchor ->
                    pendingDraft = CollectionDraft(listOf(dragged, target), anchor)
                }
            )
        }
    }

    if (pendingDraft != null) {
        val draft = pendingDraft ?: return
        val existingNames = state.collections.map { it.name }
        AlertDialog(
            onDismissRequest = { pendingDraft = null },
            title = { Text(text = stringResource(id = R.string.wallet_utxo_canvas_collection_create_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = collectionName,
                        onValueChange = { value ->
                            collectionName = value
                            collectionNameError = null
                        },
                        label = { Text(text = stringResource(id = R.string.wallet_utxo_canvas_collection_name_label)) },
                        singleLine = true,
                        isError = collectionNameError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CollectionColorPicker(
                        selectedColor = collectionColor,
                        onColorSelected = { collectionColor = it },
                        availableColors = availableColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    collectionNameError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = collectionName.trim()
                        when {
                            trimmed.isEmpty() -> {
                                collectionNameError = blankNameError
                            }
                            existingNames.any { it.equals(trimmed, ignoreCase = true) } -> {
                                collectionNameError = duplicateNameError
                            }
                            else -> {
                                onCreateCollection(trimmed, collectionColor, draft.utxos, draft.anchorIndex)
                                pendingDraft = null
                            }
                        }
                    },
                    enabled = hasAvailableColors
                ) {
                    Text(text = stringResource(id = R.string.wallet_utxo_canvas_collection_create_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDraft = null }) {
                    Text(text = stringResource(id = R.string.wallet_utxo_canvas_collection_cancel_action))
                }
            }
        )
    }
}

@Composable
private fun EmptyCanvasState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wallet_utxo_canvas_empty_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.wallet_utxo_canvas_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UtxoCanvasGrid(
    items: List<UtxoCanvasItemUi>,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onUtxoClick: (UtxoCanvasItemUi.Utxo) -> Unit,
    onCollectionClick: (UtxoCollectionUi) -> Unit,
    onReorder: (List<UtxoCanvasItemUi>) -> Unit,
    onDropOnCollection: (UtxoCanvasItemUi.Utxo, Long) -> Unit,
    onRequestCollection: (UtxoCanvasItemUi.Utxo, UtxoCanvasItemUi.Utxo, Int?) -> Unit
) {
    val scrollState = rememberScrollState()
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var hoverKey by remember { mutableStateOf<String?>(null) }
    var pendingHoverKey by remember { mutableStateOf<String?>(null) }
    var hoverStartTimeMs by remember { mutableStateOf<Long?>(null) }
    val dragEnabled = dragState == null
    val edgePadding = 16.dp
    val spacing = 16.dp
    val contentPadding = 16.dp
    val minTileSize = 92.dp
    val autoScrollEdge = 72.dp
    val autoScrollStep = 24.dp
    val hoverDwellMs = 140L
    val previewTimeThresholdMs = 140L
    val previewDistanceThreshold = 12.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = edgePadding)
    ) {
        val density = LocalDensity.current
        val previewDistanceThresholdPx = with(density) { previewDistanceThreshold.toPx() }
        val maxWidth = maxWidth
        val columns = ((maxWidth + spacing) / (minTileSize + spacing)).toInt().coerceAtLeast(2)
        val tileSize = remember(maxWidth, columns) {
            val totalSpacing = spacing * (columns - 1)
            val availableWidth = maxWidth - totalSpacing
            (availableWidth / columns).coerceAtLeast(minTileSize)
        }
        val tilePx = with(density) { tileSize.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val contentPaddingPx = with(density) { contentPadding.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val autoScrollEdgePx = with(density) { autoScrollEdge.toPx() }
        val autoScrollStepPx = with(density) { autoScrollStep.toPx() }
        val dropIndex = dragState?.pointerPosition?.let { pointer ->
            indexForPosition(pointer, columns, tilePx, spacingPx, items.size, contentPaddingPx)
        }
        val hasHoverTarget = hoverKey != null || pendingHoverKey != null
        val previewReady = dragState?.hasMetPreviewThreshold(
            distanceThresholdPx = previewDistanceThresholdPx,
            timeThresholdMs = previewTimeThresholdMs
        ) == true
        val previewDropIndex = if (previewReady && !hasHoverTarget) dropIndex else null
        val previewItems = remember(items, previewDropIndex, dragState?.itemKey, hasHoverTarget) {
            val key = dragState?.itemKey
            if (key != null && previewDropIndex != null) {
                reorderItems(items, key, previewDropIndex) ?: items
            } else {
                items
            }
        }
        val itemRects = remember(previewItems, columns, tilePx, spacingPx, contentPaddingPx) {
            buildItemRects(previewItems, columns, tilePx, spacingPx, contentPaddingPx)
        }
        val itemRectsState = rememberUpdatedState(itemRects)
        val itemsState = rememberUpdatedState(items)
        val previewItemsState = rememberUpdatedState(previewItems)
        val dropIndexState = rememberUpdatedState(dropIndex)
        val previewReadyState = rememberUpdatedState(previewReady)
        val previewDropIndexState = rememberUpdatedState(previewDropIndex)
        val columnsState = rememberUpdatedState(columns)
        val tilePxState = rememberUpdatedState(tilePx)
        val spacingPxState = rememberUpdatedState(spacingPx)
        val contentPaddingPxState = rememberUpdatedState(contentPaddingPx)
        val previewOffsets = remember(itemRects) {
            itemRects.mapValues { (_, rect) ->
                IntOffset(rect.left.roundToInt(), rect.top.roundToInt())
            }
        }

        val rows = if (items.isEmpty()) 0 else ceil(items.size / columns.toFloat()).toInt()
        val gridHeight = if (rows == 0) 0.dp else {
            tileSize * rows + spacing * (rows - 1) + (contentPadding * 2)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scrollState, enabled = dragEnabled)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            ) {
                items.forEachIndexed { index, item ->
                    val rect = itemRects[item.key] ?: return@forEachIndexed
                    val isDragging = dragState?.itemKey == item.key
                    val isHovered = hoverKey == item.key && !isDragging
                    val alpha = if (isDragging) 0f else 1f
                    val targetOffset = previewOffsets[item.key]
                        ?: IntOffset(rect.left.roundToInt(), rect.top.roundToInt())
                    val offset by animateIntOffsetAsState(
                        targetValue = targetOffset,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
                        label = "canvasTileOffset"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isHovered) 1.02f else 1f,
                        label = "canvasTileScale"
                    )
                    Box(
                        modifier = Modifier
                            .offset { offset }
                            .size(tileSize)
                            .alpha(alpha)
                            .pointerInput(item.key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { localOffset ->
                                        dragState = DragState(
                                            itemKey = item.key,
                                            origin = Offset(rect.left, rect.top),
                                            touchOffset = localOffset
                                        )
                                        hoverKey = null
                                        pendingHoverKey = null
                                        hoverStartTimeMs = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        var updated = dragState?.moveBy(dragAmount)
                                            ?: return@detectDragGesturesAfterLongPress
                                        val pointer = updated.pointerPosition
                                        val pointerViewportY = pointer.y - scrollState.value.toFloat()
                                        val scrollDelta = autoScrollDelta(
                                            pointerViewportY,
                                            viewportHeightPx,
                                            autoScrollEdgePx,
                                            autoScrollStepPx
                                        )
                                        if (scrollDelta != 0f) {
                                            val consumed = scrollState.dispatchRawDelta(scrollDelta)
                                            if (consumed != 0f) {
                                                updated = updated.moveBy(Offset(0f, consumed))
                                            }
                                        }
                                        dragState = updated
                                        val candidateHover = findHoverKey(
                                            updated.pointerPosition,
                                            itemRectsState.value,
                                            updated.itemKey
                                        )
                                        val now = SystemClock.uptimeMillis()
                                        if (candidateHover != pendingHoverKey) {
                                            pendingHoverKey = candidateHover
                                            hoverStartTimeMs = if (candidateHover != null) now else null
                                        }
                                        val hasDwelled = candidateHover != null &&
                                            hoverStartTimeMs?.let { start -> now - start >= hoverDwellMs } == true
                                        hoverKey = if (hasDwelled) candidateHover else null
                                    },
                                    onDragEnd = {
                                        val snapshot = dragState
                                        val pointer = snapshot?.pointerPosition
                                        if (snapshot != null && pointer != null) {
                                            val currentItems = itemsState.value
                                            val draggedItem =
                                                currentItems.firstOrNull { it.key == snapshot.itemKey }
                                            val targetKey = hoverKey ?: findHoverKey(
                                                pointer,
                                                itemRectsState.value,
                                                snapshot.itemKey
                                            )
                                            val targetItem = targetKey?.let { key ->
                                                currentItems.firstOrNull { it.key == key }
                                            }
                                            if (draggedItem is UtxoCanvasItemUi.Utxo &&
                                                targetItem is UtxoCanvasItemUi.Collection
                                            ) {
                                                onDropOnCollection(draggedItem, targetItem.collection.id)
                                            } else if (draggedItem is UtxoCanvasItemUi.Utxo &&
                                                targetItem is UtxoCanvasItemUi.Utxo &&
                                                draggedItem.key != targetItem.key
                                            ) {
                                                val anchorIndex = currentItems.indexOfFirst { it.key == targetItem.key }
                                                    .takeIf { it >= 0 }
                                                onRequestCollection(draggedItem, targetItem, anchorIndex)
                                            } else {
                                                val dropIndex = dropIndexState.value
                                                    ?: indexForPosition(
                                                        pointer,
                                                        columnsState.value,
                                                        tilePxState.value,
                                                        spacingPxState.value,
                                                        currentItems.size,
                                                        contentPaddingPxState.value
                                                    )
                                                val reordered = if (
                                                    previewReadyState.value &&
                                                    previewDropIndexState.value != null
                                                ) {
                                                    previewItemsState.value
                                                } else {
                                                    reorderItems(
                                                        currentItems,
                                                        snapshot.itemKey,
                                                        dropIndex
                                                    )
                                                }
                                                if (reordered != null) {
                                                    onReorder(reordered)
                                                }
                                            }
                                        }
                                        dragState = null
                                        hoverKey = null
                                        pendingHoverKey = null
                                        hoverStartTimeMs = null
                                    },
                                    onDragCancel = {
                                        dragState = null
                                        hoverKey = null
                                        pendingHoverKey = null
                                        hoverStartTimeMs = null
                                    }
                                )
                            }
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    ) {
                        CanvasItemTile(
                            item = item,
                            balanceUnit = balanceUnit,
                            balancesHidden = balancesHidden,
                            isHovered = isHovered,
                            onUtxoClick = onUtxoClick,
                            onCollectionClick = onCollectionClick
                        )
                    }
                }

                val dragSnapshot = dragState
                if (dragSnapshot != null) {
                    val draggedItem = items.firstOrNull { it.key == dragSnapshot.itemKey }
                    if (draggedItem != null) {
                        val offset = dragSnapshot.origin + dragSnapshot.dragDelta
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                                }
                                .size(tileSize)
                                .graphicsLayer(scaleX = 1.05f, scaleY = 1.05f)
                        ) {
                            CanvasItemTile(
                                item = draggedItem,
                                balanceUnit = balanceUnit,
                                balancesHidden = balancesHidden,
                                isHovered = true,
                                onUtxoClick = {},
                                onCollectionClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasItemTile(
    item: UtxoCanvasItemUi,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    isHovered: Boolean,
    onUtxoClick: (UtxoCanvasItemUi.Utxo) -> Unit,
    onCollectionClick: (UtxoCollectionUi) -> Unit
) {
    when (item) {
        is UtxoCanvasItemUi.Utxo -> {
            UtxoTileCard(
                utxo = item,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                highlighted = isHovered,
                onClick = { onUtxoClick(item) }
            )
        }
        is UtxoCanvasItemUi.Collection -> {
            CollectionTileCard(
                collection = item.collection,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                highlighted = isHovered,
                onClick = { onCollectionClick(item.collection) }
            )
        }
    }
}

@Composable
private fun UtxoTileCard(
    utxo: UtxoCanvasItemUi.Utxo,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val background = MaterialTheme.colorScheme.surfaceColorAtElevation(if (highlighted) 6.dp else 2.dp)
    val outline = if (highlighted) MaterialTheme.colorScheme.primary else Color.Transparent
    val border = if (highlighted) BorderStroke(1.dp, outline) else null
    val shortOutpoint = remember(utxo.txid, utxo.vout) {
        abbreviatedOutpointText(utxo.txid, utxo.vout)
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = background),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UtxoIdenticon(
                seed = utxo.key,
                size = 40.dp
            )
            Text(
                text = abbreviatedBalanceText(utxo.valueSats, balanceUnit, hidden = balancesHidden),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = shortOutpoint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CollectionTileCard(
    collection: UtxoCollectionUi,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val baseColor = collectionColor(collection.color)
    val contentColor = if (baseColor.luminance() > 0.5f) Color.Black else Color.White
    val outline = if (highlighted) MaterialTheme.colorScheme.primary else Color.Transparent
    val border = if (highlighted) BorderStroke(1.dp, outline) else null
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = baseColor, contentColor = contentColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = abbreviatedBalanceText(collection.totalValueSats, balanceUnit, hidden = balancesHidden),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        id = R.string.wallet_utxo_canvas_collection_count,
                        collection.memberKeys.size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun abbreviatedOutpointText(txid: String, vout: Int): String {
    val prefix = txid.take(5)
    val suffix = txid.drop(5).take(2)
    return "$prefix...$suffix:$vout"
}

private fun autoScrollDelta(
    pointerViewportY: Float,
    viewportHeightPx: Float,
    edgeThresholdPx: Float,
    maxStepPx: Float
): Float {
    if (viewportHeightPx <= 0f) return 0f
    return when {
        pointerViewportY < edgeThresholdPx -> {
            val distance = (edgeThresholdPx - pointerViewportY).coerceAtLeast(0f)
            -maxStepPx * (distance / edgeThresholdPx).coerceIn(0f, 1f)
        }
        pointerViewportY > viewportHeightPx - edgeThresholdPx -> {
            val distance = (pointerViewportY - (viewportHeightPx - edgeThresholdPx)).coerceAtLeast(0f)
            maxStepPx * (distance / edgeThresholdPx).coerceIn(0f, 1f)
        }
        else -> 0f
    }
}

private fun buildItemRects(
    items: List<UtxoCanvasItemUi>,
    columns: Int,
    tilePx: Float,
    spacingPx: Float,
    yOffsetPx: Float
): Map<String, Rect> {
    return items.mapIndexed { index, item ->
        val row = index / columns
        val col = index % columns
        val left = col * (tilePx + spacingPx)
        val top = row * (tilePx + spacingPx) + yOffsetPx
        item.key to Rect(left, top, left + tilePx, top + tilePx)
    }.toMap()
}

private fun findHoverKey(
    pointer: Offset,
    itemRects: Map<String, Rect>,
    draggingKey: String?
): String? =
    itemRects.entries.firstOrNull { entry ->
        entry.key != draggingKey && entry.value.contains(pointer)
    }?.key

private fun indexForPosition(
    pointer: Offset,
    columns: Int,
    tilePx: Float,
    spacingPx: Float,
    itemCount: Int,
    yOffsetPx: Float
): Int {
    if (itemCount == 0) return 0
    val col = floor(pointer.x / (tilePx + spacingPx)).toInt()
    val row = floor((pointer.y - yOffsetPx) / (tilePx + spacingPx)).toInt()
    val index = row * columns + col
    return index.coerceIn(0, itemCount - 1)
}

private fun reorderItems(
    items: List<UtxoCanvasItemUi>,
    draggedKey: String,
    dropIndex: Int
): List<UtxoCanvasItemUi>? {
    val fromIndex = items.indexOfFirst { it.key == draggedKey }
    if (fromIndex == -1 || fromIndex == dropIndex) return null
    val mutable = items.toMutableList()
    val item = mutable.removeAt(fromIndex)
    val clampedIndex = dropIndex.coerceIn(0, mutable.size)
    mutable.add(clampedIndex, item)
    return mutable.toList()
}

private fun availableCollectionColors(
    collections: List<UtxoCollectionUi>
): List<UtxoCollectionColor> {
    val used = collections.map { it.color }.toSet()
    return UtxoCollectionColor.entries.filterNot { it in used }
}

private fun nextCollectionColor(collections: List<UtxoCollectionUi>): UtxoCollectionColor =
    availableCollectionColors(collections).firstOrNull() ?: UtxoCollectionColor.Mint

private data class DragState(
    val itemKey: String,
    val origin: Offset,
    val touchOffset: Offset,
    val startTimeMs: Long = SystemClock.uptimeMillis(),
    val dragDelta: Offset = Offset.Zero
) {
    val pointerPosition: Offset = origin + dragDelta + touchOffset

    fun hasMetPreviewThreshold(
        distanceThresholdPx: Float,
        timeThresholdMs: Long
    ): Boolean {
        val distanceMet = distanceThresholdPx <= 0f || dragDelta.getDistance() >= distanceThresholdPx
        val timeMet = timeThresholdMs <= 0 || SystemClock.uptimeMillis() - startTimeMs >= timeThresholdMs
        return distanceMet || timeMet
    }

    fun moveBy(delta: Offset): DragState = copy(dragDelta = dragDelta + delta)
}

private data class CollectionDraft(
    val utxos: List<UtxoCanvasItemUi.Utxo>,
    val anchorIndex: Int?
)

data class UtxoCanvasUiState(
    val summary: WalletSummary? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.SATS,
    val balancesHidden: Boolean = false,
    val items: List<UtxoCanvasItemUi> = emptyList(),
    val collections: List<UtxoCollectionUi> = emptyList(),
    val utxosByKey: Map<String, UtxoCanvasItemUi.Utxo> = emptyMap(),
    val isLoading: Boolean = true
)

sealed interface UtxoCanvasItemUi {
    val key: String

    data class Utxo(
        val txid: String,
        val vout: Int,
        val valueSats: Long
    ) : UtxoCanvasItemUi {
        override val key: String = "$txid:$vout"
    }

    data class Collection(
        val collection: UtxoCollectionUi
    ) : UtxoCanvasItemUi {
        override val key: String = "collection:${collection.id}"
    }
}

data class UtxoCollectionUi(
    val id: Long,
    val name: String,
    val color: UtxoCollectionColor,
    val totalValueSats: Long,
    val memberKeys: List<String>
)

@HiltViewModel
class UtxoCanvasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletReadRepository: WalletReadRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val canvasRepository: UtxoCanvasRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val walletDetailFlow = walletReadRepository.observeWalletDetail(walletId)

    val uiState: StateFlow<UtxoCanvasUiState> = combine(
        walletDetailFlow,
        canvasRepository.observeCanvasSnapshot(walletId),
        appPreferencesRepository.balancesHidden
    ) { detail, snapshot, balancesHidden ->
        if (detail == null) {
            return@combine UtxoCanvasUiState(
                summary = null,
                balanceUnit = BalanceUnit.SATS,
                balancesHidden = balancesHidden,
                isLoading = false
            )
        }
        buildUiState(
            detail = detail,
            snapshot = snapshot,
            balancesHidden = balancesHidden
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UtxoCanvasUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            combine(
                walletDetailFlow,
                appPreferencesRepository.dustThresholdSats
            ) { detail, dustThreshold ->
                (detail?.utxos ?: emptyList()) to dustThreshold
            }.distinctUntilChanged { old, new ->
                val oldKeys = old.first.map { "${it.txid}:${it.vout}" }.toSet()
                val newKeys = new.first.map { "${it.txid}:${it.vout}" }.toSet()
                oldKeys == newKeys && old.second == new.second
            }.collect { (utxos, dustThreshold) ->
                canvasRepository.syncCanvas(walletId, utxos, dustThreshold)
            }
        }
    }

    fun updateCanvasOrder(items: List<UtxoCanvasItemUi>) {
        val ordered = items.map { item ->
            when (item) {
                is UtxoCanvasItemUi.Utxo ->
                    UtxoCanvasItemRef(UtxoCanvasItemType.UTXO, item.key)
                is UtxoCanvasItemUi.Collection ->
                    UtxoCanvasItemRef(UtxoCanvasItemType.COLLECTION, item.collection.id.toString())
            }
        }
        viewModelScope.launch {
            canvasRepository.updateCanvasOrder(walletId, ordered)
        }
    }

    fun createCollection(
        name: String,
        color: UtxoCollectionColor,
        utxos: List<UtxoCanvasItemUi.Utxo>,
        anchorIndex: Int?
    ) {
        if (utxos.isEmpty()) return
        viewModelScope.launch {
            canvasRepository.createCollection(
                walletId = walletId,
                name = name,
                color = color,
                utxos = utxos.map { UtxoRef(it.txid, it.vout) },
                anchorIndex = anchorIndex
            )
        }
    }

    fun addUtxoToCollection(utxo: UtxoCanvasItemUi.Utxo, collectionId: Long) {
        viewModelScope.launch {
            canvasRepository.addUtxoToCollection(walletId, UtxoRef(utxo.txid, utxo.vout), collectionId)
        }
    }

    fun removeUtxoFromCollection(utxo: UtxoCanvasItemUi.Utxo) {
        viewModelScope.launch {
            canvasRepository.removeUtxoFromCollection(walletId, UtxoRef(utxo.txid, utxo.vout))
        }
    }

    private fun buildUiState(
        detail: WalletDetail,
        snapshot: UtxoCanvasSnapshot,
        balancesHidden: Boolean
    ): UtxoCanvasUiState {
        val utxos = detail.utxos.associateBy({ "${it.txid}:${it.vout}" }) { utxo ->
            UtxoCanvasItemUi.Utxo(
                txid = utxo.txid,
                vout = utxo.vout,
                valueSats = utxo.valueSats
            )
        }
        val membershipMap = snapshot.memberships.groupBy { it.collectionId }
        val collections = snapshot.collections.map { collection ->
            val memberKeys = membershipMap[collection.id]
                ?.map { "${it.txid}:${it.vout}" }
                ?.filter(utxos::containsKey)
                ?: emptyList()
            val totalValue = memberKeys.sumOf { key -> utxos[key]?.valueSats ?: 0L }
            UtxoCollectionUi(
                id = collection.id,
                name = collection.name,
                color = collection.color,
                totalValueSats = totalValue,
                memberKeys = memberKeys
            )
        }
        val collectionMap = collections.associateBy { it.id }
        val orderedItems = snapshot.items
            .sortedBy { it.positionIndex }
            .mapNotNull { item ->
                when (item.type) {
                    UtxoCanvasItemType.UTXO ->
                        utxos[item.refId]?.let { UtxoCanvasItemUi.Utxo(it.txid, it.vout, it.valueSats) }
                    UtxoCanvasItemType.COLLECTION -> {
                        val collectionId = item.refId.toLongOrNull() ?: return@mapNotNull null
                        collectionMap[collectionId]?.let { UtxoCanvasItemUi.Collection(it) }
                    }
                }
            }
        return UtxoCanvasUiState(
            summary = detail.summary,
            balanceUnit = BalanceUnit.SATS,
            balancesHidden = balancesHidden,
            items = orderedItems,
            collections = collections,
            utxosByKey = utxos,
            isLoading = false
        )
    }
}
