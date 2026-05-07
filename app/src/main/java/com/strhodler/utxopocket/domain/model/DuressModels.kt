package com.strhodler.utxopocket.domain.model

const val DEFAULT_DURESS_DECOY_BALANCE_SATS: Long = 50_000L
const val MIN_DURESS_DECOY_BALANCE_SATS: Long = 54_892L
const val MAX_DURESS_DECOY_BALANCE_SATS: Long = 185_374L

sealed interface DuressSessionState {
    data object Inactive : DuressSessionState
    data class FakeActive(val decoyBalanceSats: Long = DEFAULT_DURESS_DECOY_BALANCE_SATS) :
        DuressSessionState
}
