package com.strhodler.utxopocket.presentation.wiki

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.strhodler.utxopocket.data.glossary.GlossaryRepository
import com.strhodler.utxopocket.data.wiki.WikiRepository
import com.strhodler.utxopocket.presentation.glossary.GlossaryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WikiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    wikiRepository: WikiRepository,
    glossaryRepository: GlossaryRepository
) : ViewModel() {

    private val topicId: String? = savedStateHandle[WikiNavigation.TopicIdArg]
    private val topic = topicId?.let { wikiRepository.topicById(it) }
    private val relatedTopics = topic?.relatedTopicIds
        ?.mapNotNull(wikiRepository::topicById)
        ?: emptyList()

    private val relatedGlossary: List<GlossaryEntry> = topic?.glossaryRefIds
        ?.mapNotNull(glossaryRepository::entryById)
        ?: emptyList()

    private val _uiState = MutableStateFlow(
        if (topic != null) {
            WikiDetailUiState(
                topic = topic,
                relatedTopics = relatedTopics,
                relatedGlossary = relatedGlossary
            )
        } else {
            WikiDetailUiState(topic = null, relatedTopics = emptyList(), relatedGlossary = emptyList(), isMissing = true)
        }
    )
    val uiState: StateFlow<WikiDetailUiState> = _uiState.asStateFlow()
}

data class WikiDetailUiState(
    val topic: WikiTopic?,
    val relatedTopics: List<WikiTopic>,
    val relatedGlossary: List<GlossaryEntry>,
    val isMissing: Boolean = false
)
