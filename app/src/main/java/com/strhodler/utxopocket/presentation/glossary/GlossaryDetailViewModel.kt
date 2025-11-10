package com.strhodler.utxopocket.presentation.glossary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.strhodler.utxopocket.data.glossary.GlossaryRepository
import com.strhodler.utxopocket.data.wiki.WikiRepository
import com.strhodler.utxopocket.presentation.wiki.WikiTopic
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class GlossaryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    glossaryRepository: GlossaryRepository,
    wikiRepository: WikiRepository
) : ViewModel() {

    private val entryId: String? = savedStateHandle[GlossaryNavigation.EntryIdArg]

    private val _uiState: MutableStateFlow<GlossaryDetailUiState> =
        MutableStateFlow(GlossaryDetailUiState.Loading)
    val uiState: StateFlow<GlossaryDetailUiState> = _uiState.asStateFlow()

    init {
        val id = entryId
        if (id.isNullOrBlank()) {
            _uiState.value = GlossaryDetailUiState.NotFound
        } else {
            val entry = glossaryRepository.entryById(id)
            if (entry != null) {
                val relatedWiki: List<WikiTopic> = wikiRepository
                    .categories()
                    .flatMap { it.topics }
                    .filter { topic -> topic.glossaryRefIds.contains(entry.id) }
                _uiState.value = GlossaryDetailUiState.Success(entry, relatedWiki)
            } else {
                _uiState.value = GlossaryDetailUiState.NotFound
            }
        }
    }
}

sealed interface GlossaryDetailUiState {
    data object Loading : GlossaryDetailUiState
    data class Success(val entry: GlossaryEntry, val relatedWiki: List<WikiTopic>) : GlossaryDetailUiState
    data object NotFound : GlossaryDetailUiState
}
