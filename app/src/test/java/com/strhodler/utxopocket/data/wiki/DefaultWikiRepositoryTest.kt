package com.strhodler.utxopocket.data.wiki

import com.strhodler.utxopocket.domain.model.WikiSection
import com.strhodler.utxopocket.domain.model.WikiTopic
import com.strhodler.utxopocket.domain.model.WikiTopicIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultWikiRepositoryTest {

    @Test
    fun topicByIdResolvesCriticalNavigationTopics() {
        val repository = createRepository()

        assertNotNull(repository.topicById(WikiTopicIds.DescriptorCompatibility))
        assertNotNull(repository.topicById(WikiTopicIds.BlockExplorerPrivacy))
        assertNotNull(repository.topicById(WikiTopicIds.NodeConnectivity))
        assertNotNull(repository.topicById(WikiTopicIds.PrivacyThreatModels))
    }

    @Test
    fun markdownTopicsResolveCriticalTopicIds() {
        val repository = createRepository()

        val nodeConnectivity = requireNotNull(repository.topicById(WikiTopicIds.NodeConnectivity))
        val descriptorCompatibility = requireNotNull(repository.topicById(WikiTopicIds.DescriptorCompatibility))
        val blockExplorerPrivacy = requireNotNull(repository.topicById(WikiTopicIds.BlockExplorerPrivacy))
        val privacyThreatModels = requireNotNull(repository.topicById(WikiTopicIds.PrivacyThreatModels))
        val nodeCategory = repository.categories().firstOrNull { category ->
            category.topics.any { topic -> topic.id == WikiTopicIds.NodeConnectivity }
        }

        assertEquals("Connect UtxoPocket to your node", nodeConnectivity.title)
        assertEquals("Descriptor compatibility with UtxoPocket", descriptorCompatibility.title)
        assertEquals("Block Explorer Privacy", blockExplorerPrivacy.title)
        assertEquals("Threat Models", privacyThreatModels.title)
        assertEquals("privacy-toolkit", nodeCategory?.id)
    }

    @Test
    fun topicByIdReturnsNullWhenMarkdownTopicIsMissing() {
        val repository = createRepository()
        val unknownTopicId = "why-tor"

        assertEquals(null, repository.topicById(unknownTopicId))
    }

    @Test
    fun blockExplorerTopicKeepsExpectedRelatedTopicLinks() {
        val repository = createRepository()

        val topic = requireNotNull(repository.topicById(WikiTopicIds.BlockExplorerPrivacy))

        assertEquals("Block Explorer Privacy", topic.title)
        assertTrue(topic.relatedTopicIds.contains(WikiTopicIds.PrivacyThreatModels))
        assertTrue(topic.relatedTopicIds.contains(WikiTopicIds.NodeConnectivity))
    }

    @Test
    fun mergedCatalogDoesNotContainDuplicateTopicIds() {
        val repository = createRepository()

        val topicIds = repository.categories().flatMap { category ->
            category.topics.map { topic -> topic.id }
        }

        assertEquals(topicIds.toSet().size, topicIds.size)
    }

    @Test
    fun keywordSuggestionsContainTermsFromCurrentMergedCatalog() {
        val repository = createRepository()

        val suggestions = repository.keywordSuggestions(limit = 200)

        assertTrue(suggestions.any { it.equals("descriptor", ignoreCase = true) })
        assertTrue(suggestions.any { it.equals("watch-only", ignoreCase = true) })
    }

    private fun createRepository(): DefaultWikiRepository =
        DefaultWikiRepository.fromMarkdownTopics(markdownTopicsFixture)

    private fun markdownTopic(
        id: String,
        title: String,
        categoryId: String,
        categoryTitle: String,
        keywords: List<String>,
        relatedTopicIds: List<String> = emptyList()
    ): MarkdownWikiDataSource.MarkdownTopic = MarkdownWikiDataSource.MarkdownTopic(
        topic = WikiTopic(
            id = id,
            title = title,
            summary = "$title summary",
            sections = listOf(
                WikiSection(
                    title = "Overview",
                    paragraphs = listOf("$title paragraph")
                )
            ),
            keywords = keywords,
            relatedTopicIds = relatedTopicIds,
            glossaryRefIds = emptyList()
        ),
        categoryId = categoryId,
        categoryTitle = categoryTitle,
        categoryDescription = "$categoryTitle description",
        glossaryRefs = emptyList()
    )

    private val markdownTopicsFixture: List<MarkdownWikiDataSource.MarkdownTopic> = listOf(
        markdownTopic(
            id = WikiTopicIds.NodeConnectivity,
            title = "Connect UtxoPocket to your node",
            categoryId = "privacy-toolkit",
            categoryTitle = "Privacy toolkit",
            keywords = listOf("node", "tor", "watch-only")
        ),
        markdownTopic(
            id = WikiTopicIds.DescriptorCompatibility,
            title = "Descriptor compatibility with UtxoPocket",
            categoryId = "privacy-toolkit",
            categoryTitle = "Privacy toolkit",
            keywords = listOf("descriptor", "bip389")
        ),
        markdownTopic(
            id = WikiTopicIds.BlockExplorerPrivacy,
            title = "Block Explorer Privacy",
            categoryId = "privacy-networking",
            categoryTitle = "Privacy & Networking",
            keywords = listOf("block explorer", "privacy"),
            relatedTopicIds = listOf(
                WikiTopicIds.PrivacyThreatModels,
                WikiTopicIds.NodeConnectivity
            )
        ),
        markdownTopic(
            id = WikiTopicIds.PrivacyThreatModels,
            title = "Threat Models",
            categoryId = "privacy-networking",
            categoryTitle = "Privacy & Networking",
            keywords = listOf("privacy", "threat model")
        )
    )
}
