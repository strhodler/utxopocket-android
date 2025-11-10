package com.strhodler.utxopocket.presentation.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MoreViewModel @Inject constructor(
    appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {
    val preferredNetwork: StateFlow<BitcoinNetwork> =
        appPreferencesRepository.preferredNetwork.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BitcoinNetwork.DEFAULT
        )
}

