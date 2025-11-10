package com.strhodler.utxopocket.presentation.more

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoreRoute(
    onOpenWiki: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenBitcoinPdf: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel()
) {
    SetPrimaryTopBar()
    MoreScreen(
        onOpenWiki = onOpenWiki,
        onOpenGlossary = onOpenGlossary,
        onOpenBitcoinPdf = onOpenBitcoinPdf,
        onOpenFeatures = onOpenFeatures,
        onOpenAbout = onOpenAbout,
        onOpenDisclaimer = onOpenDisclaimer,
        viewModel = viewModel
    )
}

@Composable
private fun MoreScreen(
    onOpenWiki: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenBitcoinPdf: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    viewModel: MoreViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val networkState = viewModel.preferredNetwork.collectAsStateWithLifecycle()
    val network = networkState.value
    val faucetLinks = remember(network) { faucetLinksFor(network) }
    var showFaucetDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.more_section_documents),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.more_item_bitcoin_pdf))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.pdf_viewer_title))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBitcoinPdf)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.more_item_wiki))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.more_item_wiki_supporting))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenWiki)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.more_item_glossary))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.more_item_glossary_supporting))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenGlossary)
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.more_section_about),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.more_item_features))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.more_item_features_supporting))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenFeatures)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.more_item_about))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.more_item_about_supporting))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAbout)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.more_item_disclaimer))
                    },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.more_item_disclaimer_supporting))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenDisclaimer)
                )
            }
            if (faucetLinks.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.more_section_others),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                item {
                    val networkLabel = when (network) {
                        com.strhodler.utxopocket.domain.model.BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
                        com.strhodler.utxopocket.domain.model.BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
                        com.strhodler.utxopocket.domain.model.BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
                        com.strhodler.utxopocket.domain.model.BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
                    }
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.wallets_testnet_faucet_banner_title))
                        },
                        supportingContent = {
                            Text(text = stringResource(id = R.string.wallets_testnet_faucet_banner_body, networkLabel))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFaucetDialog = true }
                    )
                }
            }
        }
    }

    if (showFaucetDialog && faucetLinks.isNotEmpty()) {
        FaucetListDialog(
            network = network,
            faucets = faucetLinks,
            onDismiss = { showFaucetDialog = false }
        )
    }
}

@Composable
fun AboutRoute(onBack: () -> Unit) {
    AboutScreen(onBack = onBack)
}

@Composable
fun FeaturesRoute(onBack: () -> Unit) {
    FeaturesScreen(onBack = onBack)
}

@Composable
private fun FeaturesScreen(onBack: () -> Unit) {
    val features = remember {
        listOf(
            FeatureItem(
                titleRes = R.string.feature_watch_title,
                descriptionRes = R.string.feature_watch_description
            ),
            FeatureItem(
                titleRes = R.string.feature_descriptors_title,
                descriptionRes = R.string.feature_descriptors_description
            ),
            FeatureItem(
                titleRes = R.string.feature_bip_support_title,
                descriptionRes = R.string.feature_bip_support_description
            ),
            FeatureItem(
                titleRes = R.string.feature_bip329_title,
                descriptionRes = R.string.feature_bip329_description
            ),
            FeatureItem(
                titleRes = R.string.feature_connectivity_title,
                descriptionRes = R.string.feature_connectivity_description
            ),
            FeatureItem(
                titleRes = R.string.feature_health_title,
                descriptionRes = R.string.feature_health_description
            )
        )
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.more_item_features),
        onBackClick = onBack
    )

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.features_screen_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(features) { feature ->
                FeatureCard(feature = feature)
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = feature.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = feature.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class FeatureItem(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

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
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        AboutDeveloperContent(
            lightningAddress = DEVELOPER_LIGHTNING_ADDRESS,
            onOpenRepository = { uriHandler.openUri(DEVELOPER_REPOSITORY_URL) },
            onLinkCopied = { message -> showSnackbar(message, SnackbarDuration.Short) },
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(innerPadding)
        )
    }
}

@Composable
private fun AboutDeveloperContent(
    lightningAddress: String,
    onOpenRepository: () -> Unit,
    onLinkCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarPainter = painterResource(id = R.drawable.strhodler)
    val lightningUri = remember(lightningAddress) { lightningAddress }
    val qrBitmap = remember(lightningUri) { generateQrBitmap(lightningUri, size = 512) }
    val copyMessage = stringResource(id = R.string.about_sheet_copy_toast)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeveloperLinkItem(
                title = stringResource(id = R.string.about_sheet_link_repository),
                value = DEVELOPER_REPOSITORY_URL,
                onOpen = onOpenRepository,
                onCopy = { onLinkCopied(copyMessage) }
            )
            DeveloperLinkItem(
                title = stringResource(id = R.string.about_sheet_link_nostr),
                value = DEVELOPER_NOSTR,
                onCopy = { onLinkCopied(copyMessage) }
            )
            DeveloperLinkItem(
                title = stringResource(id = R.string.about_sheet_link_lightning),
                value = lightningAddress,
                onCopy = { onLinkCopied(copyMessage) }
            )
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
        Image(
            painter = avatarPainter,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun DisclaimerRoute(onBack: () -> Unit) {
    DisclaimerScreen(onBack = onBack)
}

@Composable
private fun DisclaimerScreen(onBack: () -> Unit) {
    SetSecondaryTopBar(
        title = stringResource(id = R.string.more_item_disclaimer),
        onBackClick = onBack
    )

    val scrollState = rememberScrollState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.disclaimer_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_watch_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_no_advice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_security_storage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_network_trust),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_no_custody),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_no_warranty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_compliance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u2022 " + stringResource(id = R.string.disclaimer_point_backups),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.disclaimer_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class FaucetLink(
    val label: String,
    val url: String
)

private fun faucetLinksFor(network: com.strhodler.utxopocket.domain.model.BitcoinNetwork): List<FaucetLink> = when (network) {
    com.strhodler.utxopocket.domain.model.BitcoinNetwork.TESTNET -> listOf(
        FaucetLink(label = "bitcoinfaucet.uo1.net", url = "https://bitcoinfaucet.uo1.net/"),
        FaucetLink(label = "coinfaucet.eu (testnet3)", url = "https://coinfaucet.eu/en/btc-testnet/")
    )
    com.strhodler.utxopocket.domain.model.BitcoinNetwork.TESTNET4 -> listOf(
        FaucetLink(label = "faucet.testnet4.dev", url = "https://faucet.testnet4.dev/"),
        FaucetLink(label = "coinfaucet.eu (testnet4)", url = "https://coinfaucet.eu/en/btc-testnet4/"),
        FaucetLink(label = "mempool.space faucet", url = "https://mempool.space/testnet4/faucet")
    )
    else -> emptyList()
}

@Composable
private fun FaucetListDialog(
    network: com.strhodler.utxopocket.domain.model.BitcoinNetwork,
    faucets: List<FaucetLink>,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val networkLabel = when (network) {
                com.strhodler.utxopocket.domain.model.BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
                com.strhodler.utxopocket.domain.model.BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
                com.strhodler.utxopocket.domain.model.BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
                com.strhodler.utxopocket.domain.model.BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
            }
            Text(
                text = stringResource(
                    id = R.string.wallets_testnet_faucet_dialog_title,
                    networkLabel
                ),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.wallets_testnet_faucet_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                faucets.forEach { faucet ->
                    OutlinedButton(
                        onClick = { uriHandler.openUri(faucet.url) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = faucet.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.wallets_testnet_faucet_dialog_close))
            }
        }
    )
}

@Composable
private fun DeveloperLinkItem(
    title: String,
    value: String,
    onCopy: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val itemModifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .let { base -> if (onOpen != null) base.clickable(onClick = onOpen) else base }

    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(value))
                    onCopy?.invoke()
                }) {
                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                }
                if (onOpen != null) {
                    IconButton(onClick = onOpen) {
                        Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null)
                    }
                }
            }
        },
        modifier = itemModifier
    )
}

@Composable
fun PdfViewerRoute(onBack: () -> Unit) {
    PdfViewerScreen(onBack = onBack)
}

@Composable
private fun PdfViewerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
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
    val documentState by produceState<PdfDocumentState>(initialValue = PdfDocumentState.Loading, key1 = context) {
        value = runCatching { PdfDocumentState.Ready(preparePdfDocument(context)) }
            .getOrElse { error -> PdfDocumentState.Error(error.message ?: context.getString(R.string.pdf_viewer_error)) }
    }
    val cacheFile = (documentState as? PdfDocumentState.Ready)?.file

    SetSecondaryTopBar(
        title = stringResource(id = R.string.pdf_viewer_title),
        onBackClick = onBack,
        actions = {
            if (cacheFile != null) {
                IconButton(onClick = {
                    cacheFile?.let { file ->
                        shareOrDownloadPdf(context, file)?.let { message ->
                            showSnackbar(message.text, message.duration)
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = stringResource(id = R.string.pdf_download_action))
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        when (val current = documentState) {
            PdfDocumentState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(id = R.string.pdf_viewer_loading))
                    }
                }
            }

            is PdfDocumentState.Ready -> {
                var renderedFile by remember { mutableStateOf<File?>(null) }
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(innerPadding),
                    factory = { ctx ->
                        PDFView(ctx, null).apply {
                            setBackgroundColor(Color.TRANSPARENT)
                        }.also { pdfView ->
                            renderedFile = current.file
                            pdfView.fromFile(current.file)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .pageSnap(true)
                                .pageFling(true)
                                .spacing(12)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .load()
                        }
                    },
                    update = { pdfView ->
                        if (renderedFile != current.file) {
                            renderedFile = current.file
                            pdfView.fromFile(current.file)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .pageSnap(true)
                                .pageFling(true)
                                .spacing(12)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .load()
                        }
                    }
                )
            }

            is PdfDocumentState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = current.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private sealed interface PdfDocumentState {
    data object Loading : PdfDocumentState
    data class Ready(val file: File) : PdfDocumentState
    data class Error(val message: String) : PdfDocumentState
}

private data class SnackbarMessageData(
    val text: String,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

private suspend fun preparePdfDocument(context: Context): File {
    return withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "bitcoin.pdf")
        if (!cacheFile.exists()) {
            context.resources.openRawResource(R.raw.bitcoin).use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        cacheFile
    }
}

private fun shareOrDownloadPdf(context: Context, file: File): SnackbarMessageData? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val displayName = context.getString(R.string.pdf_download_filename)
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/UtxoPocket"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        val resolver = context.contentResolver
        val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (targetUri != null) {
            val saveResult = runCatching {
                resolver.openOutputStream(targetUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }.onFailure {
                resolver.delete(targetUri, null, null)
            }
            if (saveResult.isSuccess) {
                return SnackbarMessageData(
                    text = context.getString(R.string.pdf_download_saved, "Downloads/UtxoPocket"),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_download_action)))
        null
    }.getOrElse {
        SnackbarMessageData(
            text = context.getString(R.string.pdf_viewer_error),
            duration = SnackbarDuration.Long
        )
    }
}

private const val DEVELOPER_REPOSITORY_URL = "https://github.com/strhodler/utxopocket-android"
private const val DEVELOPER_LIGHTNING_ADDRESS = "strhodler@getalby.com"
private const val DEVELOPER_NOSTR = "npub1dd3k7ku95jhpyh9y7pgx9qrh2ykvtfl5lnncqzzt2gyhgw0a04ysm4paad"
