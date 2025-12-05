package com.strhodler.utxopocket.presentation.wallets.receive

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun ReceiveRoute(
    onBack: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDetailsSheet by remember { mutableStateOf(false) }
    var selectedDetail by remember { mutableStateOf<WalletAddressDetail?>(null) }
    val context = LocalContext.current
    val showSnackbar = remember(coroutineScope, snackbarHostState) {
        { message: String, duration: SnackbarDuration ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = duration,
                    withDismissAction = true
                )
            }
            Unit
        }
    }
    val copyHandler = rememberCopyToClipboard(
        successMessage = stringResource(id = R.string.receive_copy_success),
        onShowMessage = { message -> showSnackbar(message, SnackbarDuration.Short) }
    )

    SetSecondaryTopBar(
        title = stringResource(id = R.string.receive_title),
        onBackClick = onBack
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        ReceiveScreen(
            state = state,
            onCopy = { detail ->
                copyHandler(detail.value)
                viewModel.onAddressShared(detail)
            },
            onShare = { detail ->
                shareAddress(context, detail.value)?.let { message ->
                    showSnackbar(message, SnackbarDuration.Short)
                    viewModel.onAddressShared(detail)
                }
            },
            onNextAddress = viewModel::nextAddress,
            onOpenDetails = { detail ->
                selectedDetail = detail
                showDetailsSheet = true
            },
            onRetry = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }

    if (showDetailsSheet) {
        selectedDetail?.let { detail ->
            AddressDetailsBottomSheet(
                detail = detail,
                onDismiss = {
                    showDetailsSheet = false
                    selectedDetail = null
                }
            )
        }
    }
}

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val walletId: Long =
        savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
            ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
            ?: error("Wallet id is required")

    private val walletSummary: StateFlow<WalletSummary?> = walletRepository
        .observeWalletDetail(walletId)
        .map { detail -> detail?.summary }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val internalState = MutableStateFlow(ReceiveInternalState())

    val uiState: StateFlow<ReceiveUiState> = combine(
        internalState,
        walletSummary
    ) { state, summary ->
        ReceiveUiState(
            isLoading = state.isLoading,
            isAdvancing = state.isAdvancing,
            address = state.address,
            walletSummary = summary,
            error = state.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReceiveUiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            internalState.update { it.copy(isLoading = true, isAdvancing = false, error = null) }
            loadAddress(fetchAddress = ::fetchUnusedAddress)
        }
    }

    fun nextAddress() {
        if (internalState.value.isAdvancing) return
        viewModelScope.launch {
            internalState.update { it.copy(isAdvancing = true, error = null) }
            loadAddress {
                walletRepository.revealNextAddress(
                    walletId = walletId,
                    type = WalletAddressType.EXTERNAL
                )
            }
        }
    }

    fun onAddressShared(address: WalletAddressDetail) {
        viewModelScope.launch {
            runCatching {
                walletRepository.markAddressAsUsed(
                    walletId = walletId,
                    type = WalletAddressType.EXTERNAL,
                    derivationIndex = address.derivationIndex
                )
            }
        }
    }

    private suspend fun loadAddress(fetchAddress: suspend () -> WalletAddress?) {
        val addressResult = runCatching { fetchAddress() }
        val address = addressResult.getOrNull()
        if (address == null) {
            setError(addressResult.exceptionOrNull())
            return
        }
        val detailResult = runCatching {
            walletRepository.getAddressDetail(
                walletId = walletId,
                type = WalletAddressType.EXTERNAL,
                derivationIndex = address.derivationIndex
            )
        }
        val detail = detailResult.getOrNull()
        if (detail == null) {
            setError(detailResult.exceptionOrNull())
        } else {
            internalState.update {
                it.copy(
                    isLoading = false,
                    isAdvancing = false,
                    address = detail,
                    error = null
                )
            }
        }
    }

    private suspend fun fetchUnusedAddress(): WalletAddress? =
        walletRepository.listUnusedAddresses(
            walletId = walletId,
            type = WalletAddressType.EXTERNAL,
            limit = 1
        ).firstOrNull()

    private fun setError(cause: Throwable?) {
        internalState.update {
            it.copy(
                isLoading = false,
                isAdvancing = false,
                address = null,
                error = if (cause == null) ReceiveError.NoAddress else ReceiveError.Generic
            )
        }
    }
}

data class ReceiveUiState(
    val isLoading: Boolean = true,
    val isAdvancing: Boolean = false,
    val address: WalletAddressDetail? = null,
    val walletSummary: WalletSummary? = null,
    val error: ReceiveError? = null
)

private data class ReceiveInternalState(
    val isLoading: Boolean = true,
    val isAdvancing: Boolean = false,
    val address: WalletAddressDetail? = null,
    val error: ReceiveError? = null
)

sealed interface ReceiveError {
    data object NoAddress : ReceiveError
    data object Generic : ReceiveError
}

@Composable
private fun ReceiveScreen(
    state: ReceiveUiState,
    onCopy: (WalletAddressDetail) -> Unit,
    onShare: (WalletAddressDetail) -> Unit,
    onNextAddress: () -> Unit,
    onOpenDetails: (WalletAddressDetail) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.address == null -> {
            ReceiveErrorState(
                error = state.error,
                onRetry = onRetry,
                modifier = modifier
            )
        }

        else -> {
            ReceiveContent(
                address = state.address,
                isAdvancing = state.isAdvancing,
                onCopy = onCopy,
                onShare = onShare,
                onNextAddress = onNextAddress,
                onOpenDetails = onOpenDetails,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ReceiveContent(
    address: WalletAddressDetail,
    isAdvancing: Boolean,
    onCopy: (WalletAddressDetail) -> Unit,
    onShare: (WalletAddressDetail) -> Unit,
    onNextAddress: () -> Unit,
    onOpenDetails: (WalletAddressDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ReceiveAddressCard(
            address = address,
            onCopy = { onCopy(address) },
            onShare = { onShare(address) },
            onOpenDetails = { onOpenDetails(address) }
        )
        Spacer(modifier = Modifier.weight(1f, fill = true))
        PrimaryCtaButton(
            text = if (isAdvancing) {
                stringResource(id = R.string.receive_next_address_loading)
            } else {
                stringResource(id = R.string.receive_next_address)
            },
            onClick = onNextAddress,
            enabled = !isAdvancing,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            leadingIcon = if (isAdvancing) null else Icons.Outlined.ArrowForward,
            showProgress = isAdvancing
        )
    }
}

@Composable
private fun ReceiveAddressCard(
    address: WalletAddressDetail,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val qrBitmap = remember(address.value) { generateQrBitmap(address.value) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.receive_reuse_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(220.dp)
                )
            } else {
                Text(
                    text = stringResource(id = R.string.address_detail_qr_error_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SelectionContainer {
                Text(
                    text = address.value,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.receive_copy_label))
                }
                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(imageVector = Icons.Outlined.IosShare, contentDescription = null)
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.receive_share_label))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onOpenDetails) {
                    Text(text = stringResource(id = R.string.receive_open_details))
                }
            }
        }
    }
}

@Composable
private fun ReceiveErrorState(
    error: ReceiveError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        ReceiveError.Generic -> stringResource(id = R.string.receive_error_generic)
        ReceiveError.NoAddress, null -> stringResource(id = R.string.receive_error_no_address)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.receive_retry))
        }
    }
}

private val PrimaryCtaMinHeight = 64.dp
private val PrimaryCtaPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

@Composable
private fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showProgress: Boolean = false
    ) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = PrimaryCtaMinHeight),
        contentPadding = PrimaryCtaPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        when {
            showProgress -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            }

            leadingIcon != null -> {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDetailsBottomSheet(
    detail: WalletAddressDetail,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val copyHandler = rememberCopyToClipboard(
        successMessage = stringResource(id = R.string.address_detail_copy_toast),
        onShowMessage = { _ -> }
    )
    val usageText = when (detail.usage) {
        AddressUsage.NEVER -> stringResource(id = R.string.address_detail_usage_never)
        AddressUsage.ONCE -> stringResource(id = R.string.address_detail_usage_once)
        AddressUsage.MULTIPLE -> stringResource(
            id = R.string.address_detail_usage_multiple,
            detail.usageCount
        )
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.address_detail_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            DetailItem(
                label = stringResource(id = R.string.address_detail_address_label),
                value = detail.value
            )
            DetailItem(
                label = stringResource(id = R.string.address_detail_path_label),
                value = detail.derivationPath
            )
            DetailItem(
                label = stringResource(id = R.string.address_detail_index_label),
                value = detail.derivationIndex.toString()
            )
            DetailItem(
                label = stringResource(id = R.string.address_detail_usage_label),
                value = usageText
            )
            DetailItem(
                label = stringResource(id = R.string.address_detail_descriptor_label),
                value = detail.descriptor
            )
            DetailItem(
                label = stringResource(id = R.string.address_detail_script_label),
                value = detail.scriptPubKey
            )
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onCopy != null) {
                TextButton(onClick = onCopy) {
                    Text(text = stringResource(id = R.string.address_detail_copy_action_label))
                }
            }
        }
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
private fun shareAddress(context: android.content.Context, address: String): String? {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, address)
    }
    return runCatching {
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.receive_share_chooser_title)
            )
        )
    }.fold(
        onSuccess = { context.getString(R.string.receive_share_label) },
        onFailure = { null }
    )
}
