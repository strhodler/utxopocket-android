package com.strhodler.utxopocket.presentation.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.BuildConfig
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar

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
    var snackbarBottomInset by remember { mutableStateOf(0.dp) }

    val networkState = viewModel.preferredNetwork.collectAsStateWithLifecycle()
    val network = networkState.value
    val faucetLinks = remember(network) { faucetLinksFor(network) }
    var showFaucetDialog by remember { mutableStateOf(false) }
    val listItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    val supportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        snackbarHost = {
            DismissibleSnackbarHost(
                hostState = snackbarHostState,
                bottomInset = snackbarBottomInset
            )
        },
        contentWindowInsets = ScreenScaffoldInsets
    ) { paddingValues ->
        snackbarBottomInset = paddingValues.calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 16.dp + paddingValues.calculateBottomPadding()
            )
        ) {
            item {
                MoreSectionSurface {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_bitcoin_pdf))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.pdf_viewer_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenBitcoinPdf)
                    )
                    MoreSectionDivider()
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_wiki))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.more_item_wiki_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenWiki)
                    )
                    MoreSectionDivider()
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_glossary))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.more_item_glossary_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenGlossary)
                    )
                }
            }
            item {
                MoreSectionSurface {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_app_version))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(
                                    id = R.string.more_item_app_version_value,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE
                                ),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MoreSectionDivider()
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_features))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.more_item_features_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenFeatures)
                    )
                    MoreSectionDivider()
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_about))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.more_item_about_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAbout)
                    )
                    MoreSectionDivider()
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.more_item_disclaimer))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.more_item_disclaimer_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = supportingTextColor
                            )
                        },
                        colors = listItemColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenDisclaimer)
                    )
                }
            }
            if (faucetLinks.isNotEmpty()) {
                item {
                    MoreSectionSurface {
                        val networkLabel = when (network) {
                            BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
                            BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
                            BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
                            BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
                        }
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(id = R.string.wallets_testnet_faucet_banner_title))
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(id = R.string.wallets_testnet_faucet_banner_body, networkLabel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = supportingTextColor
                                )
                            },
                            colors = listItemColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFaucetDialog = true }
                        )
                    }
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

private data class FaucetLink(
    val label: String,
    val url: String
)

private fun faucetLinksFor(network: BitcoinNetwork): List<FaucetLink> = when (network) {
    BitcoinNetwork.TESTNET -> listOf(
        FaucetLink(label = "bitcoinfaucet.uo1.net", url = "https://bitcoinfaucet.uo1.net/"),
        FaucetLink(label = "coinfaucet.eu (testnet3)", url = "https://coinfaucet.eu/en/btc-testnet/")
    )
    BitcoinNetwork.TESTNET4 -> listOf(
        FaucetLink(label = "faucet.testnet4.dev", url = "https://faucet.testnet4.dev/"),
        FaucetLink(label = "coinfaucet.eu (testnet4)", url = "https://coinfaucet.eu/en/btc-testnet4/"),
        FaucetLink(label = "mempool.space faucet", url = "https://mempool.space/testnet4/faucet")
    )
    else -> emptyList()
}

@Composable
private fun FaucetListDialog(
    network: BitcoinNetwork,
    faucets: List<FaucetLink>,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val networkLabel = when (network) {
                BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
                BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
                BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
                BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
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
private fun MoreSectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun MoreSectionDivider() {
    Divider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
