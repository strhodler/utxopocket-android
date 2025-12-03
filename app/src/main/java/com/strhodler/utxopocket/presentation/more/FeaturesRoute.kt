package com.strhodler.utxopocket.presentation.more

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar

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

    Scaffold(contentWindowInsets = ScreenScaffoldInsets) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
                .padding(horizontal = 24.dp),
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors()
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
