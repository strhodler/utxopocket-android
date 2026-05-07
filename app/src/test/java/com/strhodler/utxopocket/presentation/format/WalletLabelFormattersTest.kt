package com.strhodler.utxopocket.presentation.format

import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletLabelFormattersTest {

    @Test
    fun incomingPlaceholderStatusLabelMapsUnconfirmed() {
        assertEquals(
            R.string.incoming_tx_placeholder_status_unconfirmed,
            incomingPlaceholderStatusLabelRes(IncomingTxLightStatus.UNCONFIRMED)
        )
    }

    @Test
    fun incomingPlaceholderStatusLabelMapsConfirmedLight() {
        assertEquals(
            R.string.incoming_tx_placeholder_status_confirmed_light,
            incomingPlaceholderStatusLabelRes(IncomingTxLightStatus.CONFIRMED_LIGHT)
        )
    }
}
