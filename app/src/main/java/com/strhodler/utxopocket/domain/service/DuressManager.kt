package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.DuressSessionState.FakeActive
import com.strhodler.utxopocket.domain.model.DuressSessionState.Inactive
import com.strhodler.utxopocket.domain.model.DEFAULT_DURESS_DECOY_BALANCE_SATS
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DuressManager @Inject constructor() {

    private val _state = MutableStateFlow<DuressSessionState>(Inactive)
    val state: StateFlow<DuressSessionState> = _state.asStateFlow()

    fun activateFake(decoyBalanceSats: Long = DEFAULT_DURESS_DECOY_BALANCE_SATS) {
        _state.value = FakeActive(decoyBalanceSats)
    }

    fun restore() {
        _state.value = Inactive
    }
}
