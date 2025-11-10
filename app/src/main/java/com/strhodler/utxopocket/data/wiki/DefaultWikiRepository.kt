package com.strhodler.utxopocket.data.wiki

import com.strhodler.utxopocket.presentation.wiki.WikiCategory
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import com.strhodler.utxopocket.presentation.wiki.WikiTopic
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface WikiRepository {
    fun categories(): List<WikiCategory>
    fun topicById(id: String): WikiTopic?
    fun keywordSuggestions(limit: Int = DEFAULT_SUGGESTION_LIMIT): List<String>

    companion object {
        const val DEFAULT_SUGGESTION_LIMIT = 12
    }
}

@Singleton
class DefaultWikiRepository @Inject constructor(
    markdownWikiDataSource: MarkdownWikiDataSource
) : WikiRepository {

    private val catalog: Catalog

    init {
        val fallbackCategories = WikiContent.categories
        val categoryBuilders = fallbackCategories.associate { category ->
            category.id to CategoryAccumulator(
                id = category.id,
                title = category.title,
                description = category.description,
                topics = category.topics.toMutableList()
            )
        }.toMutableMap()

        val markdownTopics = markdownWikiDataSource.loadTopics()
        val seenIds = categoryBuilders.values.flatMap { it.topics }.map { it.id }.toMutableSet()

        markdownTopics.forEach { markdown ->
            val topicId = markdown.topic.id
            if (seenIds.contains(topicId)) {
                replaceExistingTopic(categoryBuilders, markdown.topic)
            } else {
                seenIds += topicId
                val accumulator = categoryBuilders.getOrPut(markdown.categoryId) {
                    CategoryAccumulator(
                        id = markdown.categoryId,
                        title = markdown.categoryTitle ?: markdown.categoryId,
                        description = markdown.categoryDescription.orEmpty(),
                        topics = mutableListOf()
                    )
                }
                if (!markdown.categoryTitle.isNullOrBlank()) {
                    accumulator.title = markdown.categoryTitle
                }
                if (!markdown.categoryDescription.isNullOrBlank()) {
                    accumulator.description = markdown.categoryDescription
                }
                accumulator.topics += markdown.topic
            }
        }

        val mergedCategories = categoryBuilders.values
            .map { accumulator ->
                accumulator.toCategory()
            }
            .sortedBy { it.title.lowercase(Locale.ROOT) }

        val topicsById = mergedCategories
            .flatMap { it.topics }
            .associateBy { it.id }

        val keywordSuggestions = mergedCategories
            .flatMap { category -> category.topics.flatMap { it.keywords } }
            .groupBy { keyword -> keyword.lowercase(Locale.ROOT) }
            .map { (_, values) -> values.first() to values.size }
            .sortedByDescending { it.second }
            .map { it.first }

        catalog = Catalog(
            categories = mergedCategories,
            topicsById = topicsById,
            keywordSuggestions = keywordSuggestions
        )
    }

    override fun categories(): List<WikiCategory> = catalog.categories

    override fun topicById(id: String): WikiTopic? = catalog.topicsById[id]

    override fun keywordSuggestions(limit: Int): List<String> =
        catalog.keywordSuggestions.take(limit)

    private fun replaceExistingTopic(
        builders: MutableMap<String, CategoryAccumulator>,
        topic: WikiTopic
    ) {
        for (accumulator in builders.values) {
            val index = accumulator.topics.indexOfFirst { it.id == topic.id }
            if (index >= 0) {
                accumulator.topics[index] = topic
                return
            }
        }
    }

    private data class Catalog(
        val categories: List<WikiCategory>,
        val topicsById: Map<String, WikiTopic>,
        val keywordSuggestions: List<String>
    )

    private data class CategoryAccumulator(
        val id: String,
        var title: String,
        var description: String,
        val topics: MutableList<WikiTopic>
    ) {
        fun toCategory(): WikiCategory = WikiCategory(
            id = id,
            title = title,
            description = description,
            topics = topics.toList()
        )
    }
}
