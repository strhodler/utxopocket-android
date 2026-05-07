package com.strhodler.utxopocket.data.wiki

import com.strhodler.utxopocket.domain.model.WikiCategory
import com.strhodler.utxopocket.domain.model.WikiTopic
import com.strhodler.utxopocket.domain.repository.WikiRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWikiRepository private constructor(
    markdownTopics: List<MarkdownWikiDataSource.MarkdownTopic>
) : WikiRepository {

    @Inject
    constructor(markdownWikiDataSource: MarkdownWikiDataSource) : this(
        markdownTopics = markdownWikiDataSource.loadTopics()
    )

    private val catalog: Catalog

    init {
        val categoryBuilders = mutableMapOf<String, CategoryAccumulator>()

        markdownTopics.forEach { markdown ->
            removeTopicById(categoryBuilders, markdown.topic.id)

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

        val mergedCategories = categoryBuilders.values
            .map { accumulator ->
                accumulator.toCategory()
            }
            .filter { it.topics.isNotEmpty() }
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

    private fun removeTopicById(
        builders: MutableMap<String, CategoryAccumulator>,
        topicId: String
    ) {
        for (accumulator in builders.values) {
            accumulator.topics.removeAll { it.id == topicId }
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

    companion object {
        internal fun fromMarkdownTopics(
            markdownTopics: List<MarkdownWikiDataSource.MarkdownTopic>
        ): DefaultWikiRepository = DefaultWikiRepository(markdownTopics)
    }
}
