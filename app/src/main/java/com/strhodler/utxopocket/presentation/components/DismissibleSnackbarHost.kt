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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import java.util.Locale

@Composable
fun DismissibleSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val dismissDescription = stringResource(id = R.string.snackbar_dismiss_action)
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            action = {
                val label = data.visuals.actionLabel
                if (label != null) {
                    TextButton(onClick = { data.performAction() }) {
                        Text(text = label.uppercase(Locale.getDefault()))
                    }
                }
            },
            dismissAction = {
                IconButton(onClick = { data.dismiss() }) {
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
