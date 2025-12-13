package com.strhodler.utxopocket.presentation.wallets.sync

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.presentation.common.ContentSection
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSyncSettingsRoute(
    viewModel: WalletSyncSettingsViewModel,
    onBack: () -> Unit
 ) {
    var showHelpSheet by rememberSaveable { mutableStateOf(false) }
    val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SetSecondaryTopBar(
        title = stringResource(id = R.string.wallet_sync_settings_title),
        onBackClick = onBack,
        actions = {
            IconButton(onClick = { showHelpSheet = true }) {
                Icon(
                    imageVector = Icons.Outlined.Help,
                    contentDescription = stringResource(id = R.string.wallet_sync_help_action_description)
                )
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }
    WalletSyncSettingsScreen(
        state = state,
        onGapChanged = viewModel::onGapChanged,
        onSave = viewModel::saveGap,
        snackbarHostState = snackbarHostState
    )
    if (showHelpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            sheetState = helpSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_sync_help_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.wallet_sync_help_full_rescan),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.wallet_sync_help_incremental),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.wallet_sync_help_incoming),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { showHelpSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = stringResource(id = R.string.dialog_close))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WalletSyncSettingsScreen(
    state: WalletSyncSettingsUiState,
    onGapChanged: (Int) -> Unit,
    onSave: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val view = LocalView.current
    val performSliderHaptic = remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
    var gapSliderValue by rememberSaveable(state.gap) { mutableStateOf(state.gap.toFloat()) }
    var gapHapticStep by remember { mutableStateOf(state.gap) }
    LaunchedEffect(state.gap) {
        gapSliderValue = state.gap.toFloat()
        gapHapticStep = state.gap
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SaveGapBar(
                isSaving = state.isSaving,
                canSave = state.gap != state.savedGap,
                onSave = onSave
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            ContentSection(
                title = stringResource(id = R.string.wallet_sync_gap_label),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(id = R.string.wallet_sync_gap_status, state.gap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = gapSliderValue,
                            onValueChange = { newValue ->
                                val quantized = newValue.roundToInt()
                                    .coerceIn(WalletSyncPreferencesRepository.MIN_GAP, WalletSyncPreferencesRepository.MAX_GAP)
                                    .toFloat()
                                gapSliderValue = quantized
                                val steppedValue = quantized.toInt()
                                if (steppedValue != gapHapticStep) {
                                    gapHapticStep = steppedValue
                                    performSliderHaptic()
                            }
                            onGapChanged(steppedValue)
                        },
                        valueRange = WalletSyncPreferencesRepository.MIN_GAP.toFloat()..WalletSyncPreferencesRepository.MAX_GAP.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                        Text(
                            text = stringResource(id = gapHintForValue(gapSliderValue.toInt())),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SaveGapBar(
    isSaving: Boolean,
    canSave: Boolean,
    onSave: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = !isSaving && canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text(text = stringResource(id = R.string.wallet_sync_save))
                }
            }
        }
    }
}

@HiltViewModel
class WalletSyncSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val _uiState = MutableStateFlow(
        WalletSyncSettingsUiState(
            gap = WalletSyncPreferencesRepository.DEFAULT_GAP,
            savedGap = WalletSyncPreferencesRepository.DEFAULT_GAP,
            isSaving = false,
            isRescanning = false,
            message = null
        )
    )
    val uiState: StateFlow<WalletSyncSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val summary = walletRepository.observeWalletDetail(walletId).firstOrNull()?.summary
            val stored = runCatching { walletSyncPreferencesRepository.getGap(walletId) }.getOrNull()
            val resolved = resolveSyncGap(stored, summary)
            _uiState.value = _uiState.value.copy(gap = resolved, savedGap = resolved)
            walletSyncPreferencesRepository.observeGap(walletId).collect { storedGap ->
                val normalized = resolveSyncGap(storedGap, summary)
                val previous = _uiState.value
                val hasUnsavedChanges = previous.gap != previous.savedGap
                _uiState.value = previous.copy(
                    savedGap = normalized,
                    gap = if (hasUnsavedChanges) previous.gap else normalized
                )
            }
        }
    }

    fun onGapChanged(value: Int) {
        _uiState.value = _uiState.value.copy(
            gap = value.coerceIn(WalletSyncPreferencesRepository.MIN_GAP, WalletSyncPreferencesRepository.MAX_GAP)
        )
    }

    fun saveGap() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val gap = _uiState.value.gap
            val result = runCatching { walletSyncPreferencesRepository.setGap(walletId, gap) }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedGap = if (result.isSuccess) gap else _uiState.value.savedGap,
                message = result.exceptionOrNull()?.message
            )
        }
    }

    fun runFullRescan() {
        if (_uiState.value.isRescanning) return
        viewModelScope.launch {
            val gap = _uiState.value.gap
            _uiState.value = _uiState.value.copy(isRescanning = true, message = null)
            val saveResult = runCatching { walletSyncPreferencesRepository.setGap(walletId, gap) }
            val rescanResult = saveResult.mapCatching {
                walletRepository.forceFullRescan(walletId, gap)
                walletRepository.refreshWallet(walletId, SyncOperation.FullRescan)
            }
            _uiState.value = _uiState.value.copy(
                isRescanning = false,
                message = rescanResult.exceptionOrNull()?.message
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class WalletSyncSettingsUiState(
    val gap: Int,
    val savedGap: Int,
    val isSaving: Boolean,
    val isRescanning: Boolean,
    val message: String?
)

@StringRes
private fun gapHintForValue(value: Int): Int = when {
    value <= 60 -> R.string.wallet_sync_gap_hint_fast
    value <= 150 -> R.string.wallet_sync_gap_hint_balanced
    value <= 220 -> R.string.wallet_sync_gap_hint_broad
    else -> R.string.wallet_sync_gap_hint_exhaustive
}
