package com.strhodler.utxopocket.presentation.glossary

import androidx.lifecycle.ViewModel
import com.strhodler.utxopocket.data.glossary.GlossaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    glossaryRepository: GlossaryRepository
) : ViewModel() {

    private val entries = glossaryRepository.entries()
    private val indexedEntries: List<IndexedGlossaryEntry> = entries.mapIndexed { index, entry ->
        IndexedGlossaryEntry(
            entry = entry,
            originalOrder = index,
            normalizedTerm = entry.term.normalizeForSearch(),
            normalizedDescription = entry.shortDescription.normalizeForSearch(),
            normalizedDefinition = entry.definition.map { paragraph -> paragraph.normalizeForSearch() },
            normalizedAliases = entry.aliases.map { alias -> alias.normalizeForSearch() },
            normalizedKeywords = entry.keywords.map { keyword -> keyword.normalizeForSearch() }
        )
    }
    private val keywordSuggestions: List<String> = entries
        .flatMap { entry -> entry.keywords.ifEmpty { entry.aliases } }
        .groupBy { keyword -> keyword.normalizeForSearch() }
        .map { (_, values) ->
            GlossaryKeywordSuggestion(
                display = values.first(),
                count = values.size
            )
        }
        .sortedByDescending { it.count }
        .map { it.display }
        .take(12)

    private val _uiState = MutableStateFlow(
        GlossaryUiState(
            entries = entries,
            filteredEntries = entries,
            suggestions = keywordSuggestions
        )
    )
    val uiState: StateFlow<GlossaryUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val filtered = filterEntries(current.entries, query)
            current.copy(
                query = query,
                filteredEntries = filtered
            )
        }
    }

    fun onSearchSubmit(query: String) {
        val trimmed = query.trim()
        _uiState.update { current ->
            val filtered = filterEntries(current.entries, trimmed)
            current.copy(
                query = trimmed,
                filteredEntries = filtered
            )
        }
    }

    fun clearSearch() {
        _uiState.update { current ->
            current.copy(
                query = "",
                filteredEntries = current.entries
            )
        }
    }

    private fun filterEntries(
        entries: List<GlossaryEntry>,
        query: String
    ): List<GlossaryEntry> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return entries
        }

        val normalizedQuery = trimmed.normalizeForSearch()

        return indexedEntries.mapNotNull { indexed ->
            val score = indexed.score(normalizedQuery)
            if (score > 0) {
                MatchedGlossaryEntry(entry = indexed.entry, score = score, originalOrder = indexed.originalOrder)
            } else {
                null
            }
        }.sortedWith(
            compareByDescending<MatchedGlossaryEntry> { it.score }
                .thenBy { it.originalOrder }
        ).map { it.entry }
    }
}

data class GlossaryUiState(
    val entries: List<GlossaryEntry>,
    val query: String = "",
    val filteredEntries: List<GlossaryEntry> = entries,
    val suggestions: List<String> = emptyList()
)

private data class IndexedGlossaryEntry(
    val entry: GlossaryEntry,
    val originalOrder: Int,
    val normalizedTerm: String,
    val normalizedDescription: String,
    val normalizedDefinition: List<String>,
    val normalizedAliases: List<String>,
    val normalizedKeywords: List<String>
)

private data class MatchedGlossaryEntry(
    val entry: GlossaryEntry,
    val score: Int,
    val originalOrder: Int
)

private data class GlossaryKeywordSuggestion(
    val display: String,
    val count: Int
)

private fun IndexedGlossaryEntry.score(query: String): Int {
    var score = 0
    score += containsScore(normalizedTerm, query, containsWeight = 85, exactWeight = 120)
    score += listScore(normalizedAliases, query, containsWeight = 70, exactWeight = 100, maxMatches = 2)
    score += listScore(normalizedKeywords, query, containsWeight = 65, exactWeight = 95, maxMatches = 3)
    score += containsScore(normalizedDescription, query, containsWeight = 50, exactWeight = 70)
    score += listScore(normalizedDefinition, query, containsWeight = 20, exactWeight = 30, maxMatches = 1)
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
