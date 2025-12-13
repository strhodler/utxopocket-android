package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.presentation.common.QrCodeDisplayDialog
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DESCRIPTOR_CLIPBOARD_CLEAR_DELAY_MS = 60_000L

@Composable
fun WalletDescriptorsRoute(
    onBack: () -> Unit,
    viewModel: WalletDescriptorsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var descriptorForQr by remember { mutableStateOf<String?>(null) }
    val showMessage = remember(coroutineScope, snackbarHostState) {
        { message: String ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
            Unit
        }
    }
    val descriptorCopiedMessage = stringResource(id = R.string.wallet_detail_descriptor_copied_toast)
    val fingerprintCopiedMessage =
        stringResource(id = R.string.wallet_detail_master_fingerprint_copied_toast)
    val handleCopyDescriptor = rememberCopyToClipboard(
        successMessage = descriptorCopiedMessage,
        onShowMessage = showMessage,
        clearDelayMs = DESCRIPTOR_CLIPBOARD_CLEAR_DELAY_MS
    )
    val handleCopyFingerprint = rememberCopyToClipboard(
        successMessage = fingerprintCopiedMessage,
        onShowMessage = showMessage,
        clearDelayMs = DESCRIPTOR_CLIPBOARD_CLEAR_DELAY_MS
    )

    val topBarTitle = state.walletName.ifBlank { stringResource(id = R.string.wallet_detail_title) }
    SetSecondaryTopBar(title = topBarTitle, onBackClick = onBack)

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .applyScreenPadding(innerPadding)
        val descriptor = state.descriptor
        when {
            state.isLoading -> {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            state.notFound || descriptor == null -> {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_not_found),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> {
                WalletDescriptorsContent(
                    descriptor = descriptor,
                    changeDescriptor = state.changeDescriptor,
                    masterFingerprints = state.masterFingerprints,
                    onCopyDescriptor = handleCopyDescriptor,
                    onCopyFingerprint = handleCopyFingerprint,
                    onShowDescriptorQr = { descriptorForQr = it },
                    contentPadding = innerPadding
                )
            }
        }
    }

    descriptorForQr?.let { descriptorText ->
        QrCodeDisplayDialog(
            title = stringResource(id = R.string.wallet_detail_descriptor_qr_title),
            value = descriptorText,
            qrContentDescription = stringResource(id = R.string.wallet_detail_descriptor_qr_action),
            errorText = stringResource(id = R.string.wallet_detail_descriptor_qr_error),
            copyActionLabel = stringResource(id = R.string.wallet_detail_descriptor_copy_action),
            closeActionLabel = stringResource(id = R.string.wallet_detail_descriptor_qr_close),
            onCopy = { handleCopyDescriptor(descriptorText) },
            onDismiss = { descriptorForQr = null }
        )
    }
}

@Composable
private fun WalletDescriptorsContent(
    descriptor: String,
    changeDescriptor: String?,
    masterFingerprints: List<String>,
    onCopyDescriptor: (String) -> Unit,
    onCopyFingerprint: (String) -> Unit,
    onShowDescriptorQr: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val combinedDescriptor = remember(descriptor, changeDescriptor) {
        combineDescriptorBranches(descriptor, changeDescriptor)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .applyScreenPadding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DescriptorWarningBanner(
            message = stringResource(id = R.string.wallet_detail_descriptors_hint)
        )
        masterFingerprints.takeIf { it.isNotEmpty() }?.let { fingerprints ->
            val fingerprintDisplayValue = fingerprints.joinToString(separator = "\n")
            FingerprintEntry(
                label = stringResource(id = R.string.wallet_detail_master_fingerprint_label),
                value = fingerprintDisplayValue,
                onCopy = { onCopyFingerprint(fingerprintDisplayValue) }
            )
        }
        DescriptorEntry(
            label = stringResource(id = R.string.wallet_detail_descriptor_label),
            value = descriptor,
            onCopy = { onCopyDescriptor(descriptor) },
            onShowQr = { onShowDescriptorQr(descriptor) }
        )
        changeDescriptor?.takeIf { it.isNotBlank() }?.let { changeDescriptorValue ->
            DescriptorEntry(
                label = stringResource(id = R.string.wallet_detail_change_descriptor_label),
                value = changeDescriptorValue,
                onCopy = { onCopyDescriptor(changeDescriptorValue) },
                onShowQr = { onShowDescriptorQr(changeDescriptorValue) }
            )
        }
        combinedDescriptor?.let { combined ->
            DescriptorEntry(
                label = stringResource(id = R.string.wallet_detail_combined_descriptor_label),
                value = combined,
                onCopy = { onCopyDescriptor(combined) },
                onShowQr = { onShowDescriptorQr(combined) }
            )
        }
    }
}

@Composable
private fun FingerprintEntry(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(id = R.string.wallet_detail_master_fingerprint_copy_action),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DescriptorEntry(
    label: String,
    value: String,
    onCopy: () -> Unit,
    onShowQr: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(id = R.string.wallet_detail_descriptor_copy_action),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onShowQr) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode,
                            contentDescription = stringResource(id = R.string.wallet_detail_descriptor_qr_action),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DescriptorWarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

data class WalletDescriptorsUiState(
    val isLoading: Boolean = true,
    val walletName: String = "",
    val descriptor: String? = null,
    val changeDescriptor: String? = null,
    val masterFingerprints: List<String> = emptyList(),
    val notFound: Boolean = false
)

@HiltViewModel
class WalletDescriptorsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    walletRepository: WalletRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")
    private val initialWalletName: String =
        savedStateHandle.get<String>(WalletsNavigation.WalletNameArg).orEmpty()

    val uiState: StateFlow<WalletDescriptorsUiState> = walletRepository.observeWalletDetail(walletId)
        .map { detail ->
            if (detail == null) {
                WalletDescriptorsUiState(
                    isLoading = false,
                    walletName = initialWalletName,
                    notFound = true
                )
            } else {
                WalletDescriptorsUiState(
                    isLoading = false,
                    walletName = detail.summary.name.ifBlank { initialWalletName },
                    descriptor = detail.descriptor,
                    changeDescriptor = detail.changeDescriptor,
                    masterFingerprints = detail.masterFingerprints
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WalletDescriptorsUiState(walletName = initialWalletName)
        )
}
