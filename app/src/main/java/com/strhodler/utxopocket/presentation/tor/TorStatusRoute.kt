package com.strhodler.utxopocket.presentation.tor

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.components.WalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.components.onGradient
import com.strhodler.utxopocket.presentation.wallets.components.rememberWalletShimmerPhase
import com.strhodler.utxopocket.presentation.wallets.components.walletCardBackground
import com.strhodler.utxopocket.presentation.wallets.components.walletShimmer
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import com.strhodler.utxopocket.presentation.tor.TorStatusViewModel

@Composable
fun TorStatusRoute(
    status: StatusBarUiState,
    onBack: () -> Unit,
    viewModel: TorStatusViewModel = hiltViewModel()
) {
    val actionsState by viewModel.uiState.collectAsStateWithLifecycle()
    SetSecondaryTopBar(
        title = stringResource(id = R.string.tor_overview_title),
        onBackClick = onBack
    )
    TorStatusScreen(
        status = status,
        actionsState = actionsState,
        onRenewIdentity = viewModel::onRenewIdentity,
        onStartTor = viewModel::onStartTor
    )
}

@Composable
private fun TorStatusScreen(
    status: StatusBarUiState,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    onStartTor: () -> Unit
) {
    val listState = rememberLazyListState()
    Scaffold(contentWindowInsets = ScreenScaffoldInsets) { innerPadding ->
        val contentPadding = PaddingValues(bottom = 32.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item("hero") {
                TorHeroHeader(
                    status = status,
                    actionsState = actionsState,
                    onRenewIdentity = onRenewIdentity,
                    onStartTor = onStartTor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item("hero_spacing") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item("details") {
                TorConnectionDetails(
                    status = status,
                    actionsState = actionsState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun TorHeroHeader(
    status: StatusBarUiState,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    onStartTor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val heroTorStatus = remember(status.torStatus, actionsState.isStarting) {
        if (actionsState.isStarting) {
            TorStatus.Connecting()
        } else {
            status.torStatus
        }
    }
    val theme = remember(heroTorStatus) { torThemeFor(heroTorStatus, colorScheme) }
    val shimmerPhase = rememberWalletShimmerPhase(durationMillis = 3600, delayMillis = 200)
    val primaryContentColor = theme.onGradient
    val message = torStatusMessage(heroTorStatus)
    val proxyEndpoint = (status.torStatus as? TorStatus.Running)?.let { tor ->
        stringResource(
            id = R.string.tor_overview_proxy_value,
            tor.proxy.host,
            tor.proxy.port
        )
    }
    val latestLogEntry = remember(status.torLog) { latestTorLogEntry(status.torLog) }
    val (proxyBadgeLabel, proxyBadgePlaceholder) = when {
        proxyEndpoint != null -> proxyEndpoint to false
        heroTorStatus is TorStatus.Connecting -> stringResource(id = R.string.tor_overview_proxy_pending_chip) to true
        else -> stringResource(id = R.string.tor_overview_proxy_unavailable_chip) to false
    }

    Column(
        modifier = modifier
            .walletCardBackground(theme, cornerRadius = 0.dp)
            .walletShimmer(
                phase = shimmerPhase,
                cornerRadius = 0.dp,
                shimmerAlpha = 0.18f,
                highlightColor = primaryContentColor
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TorStatusHeroIcon(status = heroTorStatus, tint = primaryContentColor)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.status_tor),
                style = MaterialTheme.typography.titleLarge,
                color = primaryContentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = primaryContentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TorStatusBadge(
                label = proxyBadgeLabel,
                contentColor = primaryContentColor,
                isPlaceholder = proxyBadgePlaceholder,
                shimmerPhase = shimmerPhase
            )
        }

        Text(
            text = latestLogEntry ?: stringResource(id = R.string.tor_overview_latest_event_empty),
            style = MaterialTheme.typography.bodySmall,
            color = primaryContentColor.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val torConnected = heroTorStatus is TorStatus.Running
        val torConnecting = heroTorStatus is TorStatus.Connecting
        if (torConnected) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val renewEnabled = !actionsState.isRenewing
                TextButton(
                    onClick = onRenewIdentity,
                    enabled = renewEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = primaryContentColor,
                        disabledContentColor = primaryContentColor.copy(alpha = 0.5f)
                    )
                ) {
                    if (actionsState.isRenewing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = primaryContentColor
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.settings_tor_renew_identity),
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryContentColor
                        )
                    }
                }
            }
        } else if (torConnecting) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = primaryContentColor
                )
                Text(
                    text = stringResource(id = R.string.wallets_state_connecting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = primaryContentColor
                )
            }
        } else {
            val connectEnabled = !actionsState.isStarting
            TextButton(
                onClick = onStartTor,
                enabled = connectEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = primaryContentColor,
                    disabledContentColor = primaryContentColor.copy(alpha = 0.5f)
                )
            ) {
                if (actionsState.isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = primaryContentColor
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.tor_connect_action),
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryContentColor
                    )
                }
            }
        }
    }
}


@Composable
private fun TorConnectionDetails(
    status: StatusBarUiState,
    actionsState: TorStatusActionUiState,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val details = remember(status.torStatus, status.torLog) {
        buildTorDetails(
            resources = resources,
            torStatus = status.torStatus,
            torLog = status.torLog
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.tor_overview_details_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        details.forEach { detail ->
            TorDetailCard(detail = detail)
        }
        actionsState.errorMessageRes?.let { errorRes ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TorDetailCard(
    detail: TorDetail,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = detail.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionContainer {
                Text(
                    text = detail.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (detail.isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            detail.supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TorStatusHeroIcon(
    status: TorStatus,
    tint: Color
) {
    when (status) {
        is TorStatus.Running -> {
            Image(
                painter = painterResource(id = R.drawable.ic_tor_monochrome),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(tint)
            )
        }

        is TorStatus.Connecting -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                val progress = status.progress.coerceIn(0, 100)
                if (progress <= 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 4.dp,
                        color = tint
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = tint,
                        trackColor = tint.copy(alpha = 0.25f),
                        strokeWidth = 4.dp,
                        strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_tor_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(tint)
                )
            }
        }

        TorStatus.Stopped, is TorStatus.Error -> {
            Image(
                painter = painterResource(id = R.drawable.ic_tor_monochrome),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(tint)
            )
        }
    }
}

@Composable
private fun TorStatusBadge(
    label: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    shimmerPhase: Float? = null
) {
    val badgeModifier = if (isPlaceholder && shimmerPhase != null) {
        modifier.walletShimmer(
            phase = shimmerPhase,
            cornerRadius = 50.dp,
            shimmerAlpha = 0.35f,
            highlightColor = contentColor
        )
    } else {
        modifier
    }
    Card(
        modifier = badgeModifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.14f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .widthIn(min = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = if (isPlaceholder) 0.7f else 1f)
            )
        }
    }
}

private fun buildTorDetails(
    resources: Resources,
    torStatus: TorStatus,
    torLog: String
): List<TorDetail> {
    val proxyValue = (torStatus as? TorStatus.Running)?.let {
        resources.getString(R.string.tor_overview_proxy_value, it.proxy.host, it.proxy.port)
    } ?: resources.getString(R.string.tor_overview_proxy_unavailable)
    val bootstrapValue = when (torStatus) {
        is TorStatus.Connecting -> resources.getString(
            R.string.tor_overview_bootstrap_percent_value,
            torStatus.progress.coerceIn(0, 100)
        )
        is TorStatus.Running -> resources.getString(R.string.tor_overview_bootstrap_complete)
        else -> resources.getString(R.string.tor_overview_bootstrap_pending)
    }
    val bootstrapSupporting = if (torStatus is TorStatus.Connecting) {
        torStatus.message?.takeIf { it.isNotBlank() }
    } else {
        null
    }
    val latestLog = latestTorLogEntry(torLog)

    return listOf(
        TorDetail(
            label = resources.getString(R.string.tor_overview_proxy_label),
            value = proxyValue
        ),
        TorDetail(
            label = resources.getString(R.string.tor_overview_bootstrap_label),
            value = bootstrapValue,
            supportingText = bootstrapSupporting
        ),
        TorDetail(
            label = resources.getString(R.string.tor_overview_latest_event_label),
            value = latestLog ?: resources.getString(R.string.tor_overview_latest_event_empty)
        )
    )
}

private data class TorDetail(
    val label: String,
    val value: String,
    val supportingText: String? = null,
    val isError: Boolean = false
)

private fun latestTorLogEntry(log: String): String? =
    log.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .lastOrNull()

@Composable
private fun torStatusMessage(status: TorStatus): String = when (status) {
    is TorStatus.Running -> stringResource(id = R.string.tor_status_running)
    is TorStatus.Connecting -> status.message?.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.tor_status_connecting)
    TorStatus.Stopped -> stringResource(id = R.string.tor_status_stopped)
    is TorStatus.Error -> status.message
}

private fun torThemeFor(
    status: TorStatus,
    colorScheme: androidx.compose.material3.ColorScheme
): WalletColorTheme {
    val connectedGradient = listOf(
        Color(0xFF7D4698),
        Color(0xFF9B6CBC),
        Color(0xFF5D2F78)
    )
    val greyGradient = listOf(
        colorScheme.surfaceVariant,
        colorScheme.surface,
        colorScheme.outlineVariant
    )
    val errorGradient = listOf(
        colorScheme.error,
        colorScheme.errorContainer,
        colorScheme.error.copy(alpha = 0.85f)
    )
    val (gradient, accent) = when (status) {
        is TorStatus.Running -> connectedGradient to Color(0xFFB68ED6)
        is TorStatus.Error -> errorGradient to colorScheme.onError
        is TorStatus.Connecting -> greyGradient to colorScheme.onSurface
        TorStatus.Stopped -> greyGradient to colorScheme.onSurface
    }
    return WalletColorTheme(
        gradient = gradient,
        accent = accent
    )
}
