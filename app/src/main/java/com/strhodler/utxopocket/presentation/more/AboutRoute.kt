package com.strhodler.utxopocket.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import kotlinx.coroutines.launch

@Composable
fun AboutRoute(onBack: () -> Unit) {
    AboutScreen(onBack = onBack)
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
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
        }
    }
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    SetSecondaryTopBar(
        title = stringResource(id = R.string.more_item_about),
        onBackClick = onBack
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        AboutDeveloperContent(
            lightningAddress = DEVELOPER_LIGHTNING_ADDRESS,
            onOpenRepository = { uriHandler.openUri(DEVELOPER_REPOSITORY_URL) },
            onOpenTelegram = { uriHandler.openUri(TELEGRAM_CHANNEL_URL) },
            onLinkCopied = { message -> showSnackbar(message, SnackbarDuration.Short) },
            modifier = Modifier
                .verticalScroll(scrollState)
                .applyScreenPadding(innerPadding)
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun AboutDeveloperContent(
    lightningAddress: String,
    onOpenRepository: () -> Unit,
    onOpenTelegram: () -> Unit,
    onLinkCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarPainter = painterResource(id = R.drawable.logo_text_primary)
    val lightningUri = remember(lightningAddress) { lightningAddress }
    val qrBitmap = remember(lightningUri) { generateQrBitmap(lightningUri, size = 512) }
    val copyMessage = stringResource(id = R.string.about_sheet_copy_toast)
    val copyDeveloperLink = rememberCopyToClipboard(
        successMessage = copyMessage,
        onShowMessage = onLinkCopied
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DeveloperQrPreview(
            qrBitmap = qrBitmap,
            avatarPainter = avatarPainter,
            contentDescription = stringResource(id = R.string.about_sheet_qr_caption),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Text(
            text = stringResource(id = R.string.about_sheet_qr_caption),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        SectionCard {
            item {
                DeveloperLinkItem(
                    title = stringResource(id = R.string.about_sheet_link_lightning),
                    value = lightningAddress,
                    icon = Icons.Outlined.Link,
                    onCopy = { copyDeveloperLink(lightningAddress) }
                )
            }
            item {
                DeveloperLinkItem(
                    title = stringResource(id = R.string.about_sheet_link_repository),
                    value = DEVELOPER_REPOSITORY_URL,
                    icon = Icons.Outlined.OpenInNew,
                    onOpen = onOpenRepository,
                    onCopy = { copyDeveloperLink(DEVELOPER_REPOSITORY_URL) }
                )
            }
            item {
                DeveloperLinkItem(
                    title = stringResource(id = R.string.about_sheet_link_telegram),
                    value = TELEGRAM_CHANNEL_URL,
                    icon = Icons.Outlined.OpenInNew,
                    onOpen = onOpenTelegram,
                    onCopy = { copyDeveloperLink(TELEGRAM_CHANNEL_URL) }
                )
            }
            item {
                DeveloperLinkItem(
                    title = stringResource(id = R.string.about_sheet_link_nostr),
                    value = DEVELOPER_NOSTR,
                    icon = Icons.Outlined.Link,
                    onCopy = { copyDeveloperLink(DEVELOPER_NOSTR) }
                )
            }
        }
    }
}

@Composable
private fun DeveloperQrPreview(
    qrBitmap: ImageBitmap?,
    avatarPainter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.about_sheet_qr_error),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperLinkItem(
    title: String,
    value: String,
    icon: ImageVector,
    onCopy: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null
) {
    val itemModifier = Modifier.fillMaxWidth()
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onOpen != null) {
                    IconButton(onClick = onOpen) {
                        Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null)
                    }
                }
                IconButton(onClick = { onCopy?.invoke() }) {
                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = itemModifier
    )
}

private const val DEVELOPER_REPOSITORY_URL = "https://github.com/strhodler/utxopocket-android"
private const val DEVELOPER_LIGHTNING_ADDRESS = "strhodler@getalby.com"
private const val DEVELOPER_NOSTR = "npub1dd3k7ku95jhpyh9y7pgx9qrh2ykvtfl5lnncqzzt2gyhgw0a04ysm4paad"
private const val TELEGRAM_CHANNEL_URL = "https://t.me/+XWApNWJeCSU3Mjg8"
