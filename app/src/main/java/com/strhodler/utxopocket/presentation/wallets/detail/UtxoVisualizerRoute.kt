package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
@Composable
fun UtxoVisualizerRoute(
    onBack: () -> Unit,
    viewModel: WalletDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetSecondaryTopBar(
            title = stringResource(id = R.string.wallet_utxo_visualizer_title),
        onBackClick = onBack
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { contentPadding ->
        when {
            state.summary == null && state.errorMessage != null -> {
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

            state.summary == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(contentPadding)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    UtxoAnalysisSection(
                        histogram = state.utxoAgeHistogram,
                        treemapData = state.utxoTreemap,
                        onTreemapRangeChange = viewModel::setUtxoTreemapRange,
                        onTreemapRequested = viewModel::requestUtxoTreemap,
                        balanceUnit = state.balanceUnit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
