package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.UtxoAgeHistogram
import com.strhodler.utxopocket.domain.model.UtxoBucketDistribution
import com.strhodler.utxopocket.domain.model.UtxoSizeBucket
import com.strhodler.utxopocket.domain.model.UtxoSpendabilityBucket
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.model.WikiTopicIds
import com.strhodler.utxopocket.domain.privacy.PrivacyFinding
import kotlinx.coroutines.launch

private enum class UtxoAgeDistributionTab {
    Privacy,
    Histogram,
    Spendability,
    Size,
    Collections,
    Treemap
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UtxoAgeDistributionCard(
    walletPrivacyFindings: List<PrivacyFinding>,
    histogram: UtxoAgeHistogram,
    spendabilityDistribution: UtxoBucketDistribution<UtxoSpendabilityBucket>,
    sizeDistribution: UtxoBucketDistribution<UtxoSizeBucket>,
    collectionItems: List<WalletCollectionItem>,
    totalUtxoCount: Int,
    totalUtxoValueSats: Long,
    treemapData: UtxoTreemapData,
    onTreemapRangeChange: (LongRange) -> Unit,
    onTreemapRequested: () -> Unit,
    onOpenUtxo: (String, Int) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { UtxoAgeDistributionTab.entries.toTypedArray() }
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val tabContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    var isTreemapSliderDragging by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = tabContainerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = {},
                edgePadding = 12.dp
            ) {
                tabs.forEach { tab ->
                    val selected = pagerState.currentPage == tab.ordinal
                    val label = when (tab) {
                        UtxoAgeDistributionTab.Privacy -> stringResource(id = R.string.wallet_utxo_visualization_tab_privacy)
                        UtxoAgeDistributionTab.Histogram -> stringResource(id = R.string.wallet_utxo_visualization_tab_histogram)
                        UtxoAgeDistributionTab.Spendability -> stringResource(id = R.string.wallet_utxo_visualization_tab_spendability)
                        UtxoAgeDistributionTab.Size -> stringResource(id = R.string.wallet_utxo_visualization_tab_size)
                        UtxoAgeDistributionTab.Collections -> stringResource(id = R.string.wallet_utxo_visualization_tab_collections)
                        UtxoAgeDistributionTab.Treemap -> stringResource(id = R.string.wallet_utxo_visualization_tab_treemap)
                    }
                    Tab(
                        selected = selected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                        text = {
                            Text(
                                text = label,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                if (tabs[pagerState.currentPage] == UtxoAgeDistributionTab.Treemap) {
                    onTreemapRequested()
                } else if (isTreemapSliderDragging) {
                    isTreemapSliderDragging = false
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top,
                userScrollEnabled = !isTreemapSliderDragging
            ) { pageIndex ->
                val pageScroll = rememberScrollState()
                when (tabs[pageIndex]) {
                    UtxoAgeDistributionTab.Privacy -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletPrivacyFindingsSection(
                            findings = walletPrivacyFindings,
                            onOpenWikiTopic = {
                                onOpenWikiTopic(WikiTopicIds.WalletAnalysis)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    UtxoAgeDistributionTab.Histogram -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HistogramPage(
                            histogram = histogram,
                            balanceUnit = balanceUnit
                        )
                    }

                    UtxoAgeDistributionTab.Spendability -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpendabilityPage(
                            distribution = spendabilityDistribution,
                            balanceUnit = balanceUnit
                        )
                    }

                    UtxoAgeDistributionTab.Size -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SizePage(
                            distribution = sizeDistribution,
                            balanceUnit = balanceUnit
                        )
                    }

                    UtxoAgeDistributionTab.Collections -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CollectionsPage(
                            collectionItems = collectionItems,
                            totalUtxoCount = totalUtxoCount,
                            totalUtxoValueSats = totalUtxoValueSats,
                            balanceUnit = balanceUnit
                        )
                    }

                    UtxoAgeDistributionTab.Treemap -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TreemapPage(
                            treemapData = treemapData,
                            onTreemapRangeChange = onTreemapRangeChange,
                            onOpenUtxo = onOpenUtxo,
                            balanceUnit = balanceUnit,
                            onSliderDraggingChange = { isDragging ->
                                isTreemapSliderDragging = isDragging
                            }
                        )
                    }
                }
            }
        }
    }
}
