package com.strhodler.utxopocket.presentation.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.NodeStatus

@Composable
fun nodeStatusLabel(status: NodeStatus, isSyncing: Boolean = false): String {
    if (isSyncing) {
        return stringResource(id = R.string.wallets_state_syncing)
    }
    return when (status) {
        NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
        NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
        NodeStatus.Disconnecting -> stringResource(id = R.string.wallets_state_disconnecting)
        NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
        NodeStatus.Syncing -> stringResource(id = R.string.wallets_state_syncing)
        NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
        NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
        is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
    }
}

@Composable
fun confirmationLabel(
    confirmations: Int,
    @StringRes pendingResId: Int,
    @StringRes singleResId: Int,
    @StringRes pluralResId: Int
): String = when {
    confirmations <= 0 -> stringResource(id = pendingResId)
    confirmations == 1 -> stringResource(id = singleResId)
    else -> stringResource(id = pluralResId, confirmations)
}

@StringRes
fun incomingPlaceholderStatusLabelRes(lightStatus: IncomingTxLightStatus): Int = when (lightStatus) {
    IncomingTxLightStatus.UNCONFIRMED -> R.string.incoming_tx_placeholder_status_unconfirmed
    IncomingTxLightStatus.CONFIRMED_LIGHT -> R.string.incoming_tx_placeholder_status_confirmed_light
}
