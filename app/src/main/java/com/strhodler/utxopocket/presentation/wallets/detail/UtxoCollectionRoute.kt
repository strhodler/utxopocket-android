package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.DustCollectionName
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.theme.rememberWalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun UtxoCollectionRoute(
    onBack: () -> Unit,
    onOpenUtxo: (String, Int) -> Unit,
    viewModel: UtxoCollectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(id = R.string.wallet_utxo_canvas_collection_removed)
    val fallbackTitle = stringResource(id = R.string.wallet_utxo_collection_title)
    val collectionName = state.collection?.name ?: fallbackTitle
    val isDustCollection = state.collection?.name?.equals(DustCollectionName, ignoreCase = true) == true

    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf(UtxoCollectionColor.Mint) }
    var editError by remember { mutableStateOf<String?>(null) }
    val blankNameError = stringResource(id = R.string.wallet_utxo_canvas_collection_name_error_blank)
    val duplicateNameError = stringResource(id = R.string.wallet_utxo_canvas_collection_name_error_exists)

    LaunchedEffect(showEditDialog, state.collection?.id) {
        if (showEditDialog) {
            state.collection?.let { collection ->
                editName = collection.name
                editColor = collection.color
                editError = null
            }
        }
    }

    SetSecondaryTopBar(
        title = collectionName,
        onBackClick = onBack,
        actions = {
            if (state.collection != null) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(id = R.string.wallet_utxo_collection_menu_overflow)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_utxo_collection_menu_edit)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets,
        snackbarHost = { DismissibleSnackbarHost(snackbarHostState) }
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
            state.collection == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_utxo_collection_not_found),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                UtxoCollectionScreen(
                    state = state,
                    onOpenUtxo = onOpenUtxo,
                    onRemoveUtxo = { utxo ->
                        viewModel.removeUtxoFromCollection(utxo)
                        scope.launch { snackbarHostState.showSnackbar(removedMessage) }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding)
                )
            }
        }
    }

    if (showEditDialog && state.collection != null) {
        val current = requireNotNull(state.collection)
        val usedColors = state.collections
            .filter { it.id != current.id }
            .map { it.color }
            .toSet()
        val availableColors = UtxoCollectionColor.entries
            .filter { it == current.color || it !in usedColors }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(text = stringResource(id = R.string.wallet_utxo_collection_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = editName,
                        onValueChange = { value ->
                            editName = value
                            editError = null
                        },
                        enabled = !isDustCollection,
                        label = { Text(text = stringResource(id = R.string.wallet_utxo_canvas_collection_name_label)) },
                        singleLine = true,
                        isError = editError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CollectionColorPicker(
                        selectedColor = editColor,
                        onColorSelected = { editColor = it },
                        availableColors = availableColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    editError?.let { error ->
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
                        val trimmed = if (isDustCollection) current.name else editName.trim()
                        val hasDuplicate = state.collections.any {
                            it.id != current.id && it.name.equals(trimmed, ignoreCase = true)
                        }
                        when {
                            trimmed.isEmpty() -> editError = blankNameError
                            hasDuplicate -> editError = duplicateNameError
                            else -> {
                                scope.launch {
                                    val updated = viewModel.updateCollection(trimmed, editColor)
                                    if (updated) {
                                        showEditDialog = false
                                    } else {
                                        editError = duplicateNameError
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.wallet_utxo_collection_edit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(text = stringResource(id = R.string.wallet_utxo_canvas_collection_cancel_action))
                }
            }
        )
    }
}

@Composable
private fun UtxoCollectionScreen(
    state: UtxoCollectionDetailUiState,
    onOpenUtxo: (String, Int) -> Unit,
    onRemoveUtxo: (WalletUtxo) -> Unit,
    modifier: Modifier = Modifier
) {
    val collection = state.collection ?: return
    val summary = state.summary ?: return
    val walletTheme = rememberWalletColorTheme(summary.color)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "collection_header") {
            CollectionDetailHeader(
                collection = collection,
                balanceUnit = state.balanceUnit,
                balancesHidden = state.balancesHidden,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.utxos.isEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.wallet_utxo_collection_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        } else {
            items(state.utxos, key = { "${it.txid}:${it.vout}" }) { utxo ->
                UtxoDetailedCard(
                    utxo = utxo,
                    unit = state.balanceUnit,
                    balancesHidden = state.balancesHidden,
                    dustThresholdSats = state.dustThresholdSats,
                    palette = walletTheme,
                    onClick = { onOpenUtxo(utxo.txid, utxo.vout) },
                    trailingContent = {
                        IconButton(onClick = { onRemoveUtxo(utxo) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(
                                    id = R.string.wallet_utxo_canvas_collection_remove
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CollectionDetailHeader(
    collection: UtxoCollectionUi,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RollingBalanceText(
                balanceSats = collection.totalValueSats,
                unit = balanceUnit,
                hidden = balancesHidden,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                autoScale = true
            )
            UtxoCountBadge(
                count = collection.memberKeys.size
            )
        }
    }
}

@Composable
private fun UtxoCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.wallet_utxo_canvas_collection_count, count),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

data class UtxoCollectionDetailUiState(
    val summary: WalletSummary? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.BTC,
    val balancesHidden: Boolean = false,
    val collection: UtxoCollectionUi? = null,
    val utxos: List<WalletUtxo> = emptyList(),
    val collections: List<UtxoCollectionUi> = emptyList(),
    val dustThresholdSats: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class UtxoCollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val canvasRepository: UtxoCanvasRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")
    private val collectionId: Long = savedStateHandle.get<Long>(WalletsNavigation.UtxoCollectionIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.UtxoCollectionIdArg)?.toLongOrNull()
        ?: error("Collection id is required")

    private val walletDetailFlow = walletRepository.observeWalletDetail(walletId)

    val uiState: StateFlow<UtxoCollectionDetailUiState> = combine(
        walletDetailFlow,
        canvasRepository.observeCanvasSnapshot(walletId),
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.dustThresholdSats
    ) { detail, snapshot, balanceUnit, balancesHidden, dustThresholdSats ->
        if (detail == null) {
            return@combine UtxoCollectionDetailUiState(
                summary = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                dustThresholdSats = dustThresholdSats,
                isLoading = false
            )
        }
        buildUiState(
            detail = detail,
            snapshot = snapshot,
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden,
            dustThresholdSats = dustThresholdSats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UtxoCollectionDetailUiState(isLoading = true)
    )

    fun removeUtxoFromCollection(utxo: WalletUtxo) {
        viewModelScope.launch {
            canvasRepository.removeUtxoFromCollection(walletId, UtxoRef(utxo.txid, utxo.vout))
        }
    }

    suspend fun updateCollection(name: String, color: UtxoCollectionColor): Boolean =
        canvasRepository.updateCollection(walletId, collectionId, name, color)

    private fun buildUiState(
        detail: WalletDetail,
        snapshot: UtxoCanvasSnapshot,
        balanceUnit: BalanceUnit,
        balancesHidden: Boolean,
        dustThresholdSats: Long
    ): UtxoCollectionDetailUiState {
        val utxos = detail.utxos.associateBy { "${it.txid}:${it.vout}" }
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
        val collection = collections.firstOrNull { it.id == collectionId }
        val collectionUtxos = collection?.memberKeys?.mapNotNull(utxos::get).orEmpty()
        return UtxoCollectionDetailUiState(
            summary = detail.summary,
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden,
            collection = collection,
            utxos = collectionUtxos,
            collections = collections,
            dustThresholdSats = dustThresholdSats,
            isLoading = false
        )
    }
}
