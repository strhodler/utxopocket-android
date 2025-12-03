package com.strhodler.utxopocket.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onGetStarted() {
        _uiState.update { it.copy(step = OnboardingStep.SlideOne) }
    }

    fun onPrevious() {
        _uiState.update { state ->
            val previous = state.step.previous()
            if (previous == state.step) state else state.copy(step = previous)
        }
    }

    fun onNext() {
        _uiState.update { state ->
            val next = state.step.next()
            if (next == state.step) state else state.copy(step = next)
        }
    }

    fun onSlideIndexSelected(index: Int) {
        val targetStep = OnboardingStep.fromSlideIndex(index)
        _uiState.update { state ->
            if (state.step == targetStep) state else state.copy(step = targetStep)
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        _uiState.update { it.copy(selectedNetwork = network) }
    }

    fun complete(onFinished: () -> Unit) {
        viewModelScope.launch {
            val preferredNetwork = _uiState.value.selectedNetwork
            appPreferencesRepository.setPreferredNetwork(preferredNetwork)
            appPreferencesRepository.setOnboardingCompleted(true)
            onFinished()
        }
    }
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT
)

enum class OnboardingStep {
    Welcome,
    SlideOne,
    SlideTwo,
    SlideThree,
    SlideFour,
    SlideFive,
    SlideNetwork;

    fun next(): OnboardingStep = when (this) {
        Welcome -> SlideOne
        SlideOne -> SlideTwo
        SlideTwo -> SlideThree
        SlideThree -> SlideFour
        SlideFour -> SlideFive
        SlideFive -> SlideNetwork
        SlideNetwork -> SlideNetwork
    }

    fun previous(): OnboardingStep = when (this) {
        Welcome -> Welcome
        SlideOne -> Welcome
        SlideTwo -> SlideOne
        SlideThree -> SlideTwo
        SlideFour -> SlideThree
        SlideFive -> SlideFour
        SlideNetwork -> SlideFive
    }

    fun toSlideIndex(): Int? = when (this) {
        SlideOne -> 0
        SlideTwo -> 1
        SlideThree -> 2
        SlideFour -> 3
        SlideFive -> 4
        SlideNetwork -> 5
        Welcome -> null
    }

    companion object {
        fun fromSlideIndex(index: Int): OnboardingStep = when (index.coerceIn(0, 5)) {
            0 -> SlideOne
            1 -> SlideTwo
            2 -> SlideThree
            3 -> SlideFour
            4 -> SlideFive
            else -> SlideNetwork
        }
    }
}
