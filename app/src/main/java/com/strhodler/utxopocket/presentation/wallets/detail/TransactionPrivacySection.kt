package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.privacy.PrivacyFinding

@Composable
internal fun TransactionPrivacySection(
    findings: List<PrivacyFinding>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (findings.isEmpty()) {
            Text(
                text = stringResource(R.string.transaction_privacy_section_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        findings.forEach { finding ->
            PrivacyFindingCard(
                finding = finding,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
