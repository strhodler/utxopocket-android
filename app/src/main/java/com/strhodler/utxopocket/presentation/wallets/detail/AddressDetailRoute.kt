package com.strhodler.utxopocket.presentation.wallets.detail
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun AddressDetailRoute(
    onBack: () -> Unit,
    viewModel: AddressDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
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

    SetSecondaryTopBar(
        title = stringResource(id = R.string.address_detail_title),
        onBackClick = onBack
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        AddressDetailScreen(
            state = state,
            onShowMessage = showSnackbar,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@HiltViewModel
class AddressDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val walletId: Long =
        savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
            ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
            ?: error("Wallet id is required")

    private val addressType: WalletAddressType =
        savedStateHandle.get<String>(WalletsNavigation.AddressTypeArg)
            ?.let { runCatching { WalletAddressType.valueOf(it) }.getOrNull() }
            ?: error("Address type is required")

    private val derivationIndex: Int =
        savedStateHandle.get<Int>(WalletsNavigation.AddressIndexArg)
            ?: savedStateHandle.get<String>(WalletsNavigation.AddressIndexArg)?.toIntOrNull()
            ?: error("Derivation index is required")

    private val initialAddressValue: String? =
        savedStateHandle.get<String>(WalletsNavigation.AddressValueArg)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(AddressDetailUiState(initialAddress = initialAddressValue))
    val uiState: StateFlow<AddressDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.copy(isLoading = true, error = null)
            val detail = runCatching {
                walletRepository.getAddressDetail(walletId, addressType, derivationIndex)
            }.getOrNull()
            _uiState.value = if (detail != null) {
                AddressDetailUiState(
                    isLoading = false,
                    detail = detail,
                    error = null,
                    initialAddress = null
                )
            } else {
                previous.copy(
                    isLoading = false,
                    detail = null,
                    error = AddressDetailError.NotFound
                )
            }
        }
    }
}

data class AddressDetailUiState(
    val isLoading: Boolean = true,
    val detail: WalletAddressDetail? = null,
    val error: AddressDetailError? = null,
    val initialAddress: String? = null
)

sealed interface AddressDetailError {
    data object NotFound : AddressDetailError
}

@Composable
private fun AddressDetailScreen(
    state: AddressDetailUiState,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.detail == null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                val placeholder = state.initialAddress
                Text(
                    text = placeholder ?: stringResource(id = R.string.address_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        else -> {
            AddressDetailContent(
                detail = state.detail,
                onShowMessage = onShowMessage,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AddressDetailContent(
    detail: WalletAddressDetail,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AddressDetailHeader(detail)
        AddressOverviewCard(detail, onShowMessage = onShowMessage)
        AddressScriptCard(detail, onShowMessage = onShowMessage)
    }
}

@Composable
private fun AddressDetailHeader(detail: WalletAddressDetail) {
    val qrImage = remember(detail.value) { generateQrBitmap(detail.value) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (qrImage != null) {
                Image(
                    bitmap = qrImage,
                    contentDescription = stringResource(id = R.string.address_detail_qr_content_description),
                    modifier = Modifier.size(220.dp)
                )
            } else {
                Text(
                    text = stringResource(id = R.string.address_detail_qr_error_placeholder),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SelectionText(value = detail.value, style = MaterialTheme.typography.titleMedium)

            val warningMessages = buildList {
                if (detail.type == WalletAddressType.CHANGE) {
                    add(stringResource(id = R.string.address_detail_change_warning))
                }
                when (detail.usage) {
                    AddressUsage.NEVER -> Unit
                    AddressUsage.ONCE -> add(stringResource(id = R.string.address_detail_used_warning_once))
                    AddressUsage.MULTIPLE -> add(
                        stringResource(
                            id = R.string.address_detail_used_warning_multiple,
                            detail.usageCount
                        )
                    )
                }
            }
            if (warningMessages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    warningMessages.forEach { message ->
                        AddressWarning(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressWarning(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AddressOverviewCard(
    detail: WalletAddressDetail,
    onShowMessage: (String, SnackbarDuration) -> Unit
) {
    val copyToast = stringResource(id = R.string.address_detail_copy_toast)
    val copyHandler = rememberCopyToClipboard(
        successMessage = copyToast,
        onShowMessage = { message -> onShowMessage(message, SnackbarDuration.Short) }
    )
    val usageText = when (detail.usage) {
        AddressUsage.NEVER -> stringResource(id = R.string.address_detail_usage_never)
        AddressUsage.ONCE -> stringResource(id = R.string.address_detail_usage_once)
        AddressUsage.MULTIPLE -> stringResource(
            id = R.string.address_detail_usage_multiple,
            detail.usageCount
        )
    }

    SectionCard(title = stringResource(id = R.string.address_detail_section_overview)) {
        CopyableValueRow(
            label = stringResource(id = R.string.address_detail_address_label),
            value = detail.value,
            copyContentDescription = stringResource(id = R.string.address_detail_copy_address)
        ) {
            copyHandler(it)
        }
        InfoTextRow(
            label = stringResource(id = R.string.address_detail_path_label),
            value = detail.derivationPath
        )
        InfoTextRow(
            label = stringResource(id = R.string.address_detail_index_label),
            value = detail.derivationIndex.toString()
        )
        InfoTextRow(
            label = stringResource(id = R.string.address_detail_usage_label),
            value = usageText
        )
    }
}

@Composable
private fun AddressScriptCard(
    detail: WalletAddressDetail,
    onShowMessage: (String, SnackbarDuration) -> Unit
) {
    val copyToast = stringResource(id = R.string.address_detail_copy_toast)
    val copyHandler = rememberCopyToClipboard(
        successMessage = copyToast,
        onShowMessage = { message -> onShowMessage(message, SnackbarDuration.Short) }
    )

    SectionCard(title = stringResource(id = R.string.address_detail_section_scripts)) {
        CopyableTextBlock(
            label = stringResource(id = R.string.address_detail_script_label),
            value = detail.scriptPubKey,
            copyContentDescription = stringResource(id = R.string.address_detail_copy_script)
        ) {
            copyHandler(it)
        }
        CopyableTextBlock(
            label = stringResource(id = R.string.address_detail_descriptor_label),
            value = detail.descriptor,
            copyContentDescription = stringResource(id = R.string.address_detail_copy_descriptor)
        ) {
            copyHandler(it)
        }
    }
}

@Composable
private fun CopyableValueRow(
    label: String,
    value: String,
    copyContentDescription: String,
    onCopy: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionText(
                value = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
        IconButton(onClick = { onCopy(value) }) {
            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = copyContentDescription)
        }
    }
}

@Composable
private fun CopyableTextBlock(
    label: String,
    value: String,
    copyContentDescription: String,
    onCopy: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SelectionText(value = value, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onCopy(value) }) {
                        Text(text = stringResource(id = R.string.address_detail_copy_action_label))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTextRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SelectionText(value: String, style: TextStyle) {
    SelectionContainer {
        Text(text = value, style = style)
    }
}
