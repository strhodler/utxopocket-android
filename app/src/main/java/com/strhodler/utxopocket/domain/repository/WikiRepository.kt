package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.WikiCategory
import com.strhodler.utxopocket.domain.model.WikiTopic

interface WikiRepository {
    fun categories(): List<WikiCategory>
    fun topicById(id: String): WikiTopic?
    fun keywordSuggestions(limit: Int = DEFAULT_SUGGESTION_LIMIT): List<String>

    companion object {
        const val DEFAULT_SUGGESTION_LIMIT = 12
    }
}
