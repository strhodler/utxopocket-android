package com.strhodler.utxopocket.presentation.wiki

import androidx.lifecycle.ViewModel
import com.strhodler.utxopocket.data.wiki.WikiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class WikiViewModel @Inject constructor(
    wikiRepository: WikiRepository
) : ViewModel() {

    private val categories = wikiRepository.categories()
    private val indexedCategories: List<IndexedWikiCategory> = categories.map { category ->
        IndexedWikiCategory(
            category = category,
            topics = category.topics.mapIndexed { index, topic ->
                IndexedWikiTopic(
                    topic = topic,
                    originalOrder = index,
                    normalizedTitle = topic.title.normalizeForSearch(),
                    normalizedSummary = topic.summary.normalizeForSearch(),
                    normalizedKeywords = topic.keywords.map { it.normalizeForSearch() },
                    normalizedSectionTitles = topic.sections.mapNotNull { it.title?.normalizeForSearch() },
                    normalizedParagraphs = topic.sections.flatMap { section ->
                        section.paragraphs.map { paragraph -> paragraph.normalizeForSearch() }
                    }
                )
            }
        )
    }
    private val keywordSuggestions: List<String> = wikiRepository.keywordSuggestions()

    private val _uiState = MutableStateFlow(
        WikiUiState(
            categories = categories,
            filteredCategories = categories,
            suggestions = keywordSuggestions
        )
    )
    val uiState: StateFlow<WikiUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val filtered = filterCategories(current.categories, query)
            current.copy(
                query = query,
                filteredCategories = filtered
            )
        }
    }

    fun onSearchSubmit(query: String) {
        val trimmed = query.trim()
        _uiState.update { current ->
            val filtered = filterCategories(current.categories, trimmed)
            current.copy(
                query = trimmed,
                filteredCategories = filtered
            )
        }
    }

    fun clearSearch() {
        _uiState.update { current ->
            current.copy(
                query = "",
                filteredCategories = current.categories
            )
        }
    }

    private fun filterCategories(
        categories: List<WikiCategory>,
        query: String
    ): List<WikiCategory> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return categories
        }
        val normalizedQuery = trimmed.normalizeForSearch()
        return indexedCategories.mapNotNull { indexedCategory ->
            val matchingTopics = indexedCategory.topics.mapNotNull { indexedTopic ->
                val score = indexedTopic.score(normalizedQuery)
                if (score > 0) {
                    MatchedWikiTopic(topic = indexedTopic.topic, score = score, originalOrder = indexedTopic.originalOrder)
                } else {
                    null
                }
            }.sortedWith(
                compareByDescending<MatchedWikiTopic> { it.score }
                    .thenBy { it.originalOrder }
            )

            if (matchingTopics.isEmpty()) {
                null
            } else {
                indexedCategory.category.copy(
                    topics = matchingTopics.map { it.topic }
                )
            }
        }
    }
}

data class WikiUiState(
    val categories: List<WikiCategory>,
    val query: String = "",
    val filteredCategories: List<WikiCategory> = categories,
    val suggestions: List<String> = emptyList()
)

private data class IndexedWikiCategory(
    val category: WikiCategory,
    val topics: List<IndexedWikiTopic>
)

private data class IndexedWikiTopic(
    val topic: WikiTopic,
    val originalOrder: Int,
    val normalizedTitle: String,
    val normalizedSummary: String,
    val normalizedKeywords: List<String>,
    val normalizedSectionTitles: List<String>,
    val normalizedParagraphs: List<String>
)

private data class MatchedWikiTopic(
    val topic: WikiTopic,
    val score: Int,
    val originalOrder: Int
)

private fun IndexedWikiTopic.score(query: String): Int {
    var score = 0
    score += containsScore(normalizedTitle, query, containsWeight = 80, exactWeight = 120)
    score += listScore(normalizedKeywords, query, containsWeight = 60, exactWeight = 90, maxMatches = 2)
    score += containsScore(normalizedSummary, query, containsWeight = 45, exactWeight = 70)
    score += listScore(normalizedSectionTitles, query, containsWeight = 35, exactWeight = 55, maxMatches = 2)
    score += listScore(normalizedParagraphs, query, containsWeight = 15, exactWeight = 25, maxMatches = 1)
    return score
}

private fun containsScore(
    value: String,
    query: String,
    containsWeight: Int,
    exactWeight: Int
): Int {
    if (value.isEmpty() || query.isEmpty()) return 0
    return when {
        value == query -> exactWeight
        value.contains(query) -> containsWeight
        else -> 0
    }
}

private fun listScore(
    values: List<String>,
    query: String,
    containsWeight: Int,
    exactWeight: Int,
    maxMatches: Int
): Int {
    if (values.isEmpty() || maxMatches <= 0) return 0
    var remaining = maxMatches
    var total = 0
    for (value in values) {
        if (remaining == 0) break
        val contribution = containsScore(value, query, containsWeight, exactWeight)
        if (contribution > 0) {
            total += contribution
            remaining--
        }
    }
    return total
}

private fun String.normalizeForSearch(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return DIACRITICS_REGEX.replace(normalized, "").lowercase(Locale.ROOT)
}

private val DIACRITICS_REGEX = "\\p{Mn}+".toRegex()
