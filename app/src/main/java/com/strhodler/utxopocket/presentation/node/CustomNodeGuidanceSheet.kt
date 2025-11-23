package com.strhodler.utxopocket.presentation.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomNodeGuidanceBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp) {
        configuration.screenHeightDp.dp * 0.9f
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CustomNodeGuidanceSheetContent(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        )
    }
}

@Composable
fun CustomNodeGuidanceSheetContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.node_custom_info_title),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(id = R.string.node_custom_info_description),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        val sections = listOf(
            R.string.node_custom_info_host_title to R.string.node_custom_info_host_body,
            R.string.node_custom_info_tor_title to R.string.node_custom_info_tor_body,
            R.string.node_custom_info_ssl_title to R.string.node_custom_info_ssl_body,
            R.string.node_custom_info_local_title to R.string.node_custom_info_local_body
        )
        sections.forEach { (titleRes, bodyRes) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = titleRes),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = bodyRes),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
