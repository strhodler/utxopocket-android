package com.strhodler.utxopocket.domain.model

data class WikiCategory(
    val id: String,
    val title: String,
    val description: String,
    val topics: List<WikiTopic>
)

data class WikiTopic(
    val id: String,
    val title: String,
    val summary: String,
    val sections: List<WikiSection>,
    val keywords: List<String> = emptyList(),
    val relatedTopicIds: List<String> = emptyList(),
    val glossaryRefIds: List<String> = emptyList()
)

data class WikiSection(
    val title: String?,
    val paragraphs: List<String>
)
