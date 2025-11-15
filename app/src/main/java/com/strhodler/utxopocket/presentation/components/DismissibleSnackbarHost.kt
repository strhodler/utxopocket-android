package com.strhodler.utxopocket.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import com.strhodler.utxopocket.R
import java.util.Locale

@Composable
fun DismissibleSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    val dismissDescription = stringResource(id = R.string.snackbar_dismiss_action)
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.fillMaxWidth()
    ) { data ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + bottomInset),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                modifier = Modifier
                    .widthIn(max = 520.dp),
                action = {
                    val label = data.visuals.actionLabel
                    if (label != null) {
                        TextButton(onClick = { data.performAction() }) {
                            Text(text = label.uppercase(Locale.getDefault()))
                        }
                    }
                },
                dismissAction = {
                    IconButton(
                        onClick = { data.dismiss() },
                        enabled = true
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = dismissDescription
                        )
                    }
                }
            ) {
                Text(text = data.visuals.message)
            }
        }
    }
}
