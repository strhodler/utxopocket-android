package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.privacy.PrivacyFinding
import com.strhodler.utxopocket.domain.privacy.PrivacySummary

@Composable
internal fun TransactionPrivacySection(
    summary: PrivacySummary,
    findings: List<PrivacyFinding>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.transaction_privacy_section_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.wallet_privacy_summary_total, summary.totalFindings),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (findings.isEmpty()) {
            Text(
                text = stringResource(R.string.transaction_privacy_section_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        if (summary.entries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summary.entries.forEach { entry ->
                    Text(
                        text = stringResource(
                            R.string.wallet_privacy_summary_entry,
                            entry.severity.toUiLabel(),
                            entry.count
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        findings.forEach { finding ->
            PrivacyFindingCard(
                finding = finding,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
