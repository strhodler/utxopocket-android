package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.privacy.PrivacyFinding

@Composable
internal fun WalletPrivacyFindingsSection(
    findings: List<PrivacyFinding>,
    onOpenWikiTopic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenWikiTopic)
        ) {
            ListItem(
                headlineContent = {
                    Text(text = stringResource(id = R.string.wallet_privacy_wiki_link_title))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.wallet_privacy_wiki_link_supporting))
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        if (findings.isEmpty()) {
            Text(
                text = stringResource(R.string.wallet_privacy_findings_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                findings.forEach { finding ->
                    PrivacyFindingCard(
                        finding = finding,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
