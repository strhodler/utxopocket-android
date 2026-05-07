package com.strhodler.utxopocket.data.wiki

import android.content.Context
import com.strhodler.utxopocket.data.content.markdown.FrontMatterParser
import com.strhodler.utxopocket.data.content.markdown.MarkdownAssetTreeReader
import com.strhodler.utxopocket.data.content.markdown.MarkdownFormattingSanitizer
import com.strhodler.utxopocket.domain.model.WikiSection
import com.strhodler.utxopocket.domain.model.WikiTopic
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkdownWikiDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun loadTopics(): List<MarkdownTopic> {
        val assetManager = context.assets
        val markdownFiles = MarkdownAssetTreeReader.collectMarkdownFiles(assetManager, WIKI_ROOT)
        return markdownFiles.mapNotNull { relativePath ->
            val fullPath = if (relativePath.isEmpty()) WIKI_ROOT else "$WIKI_ROOT/$relativePath"
            runCatching {
                assetManager.open(fullPath).bufferedReader().use(BufferedReader::readText)
            }.getOrNull()?.let { raw ->
                parseMarkdownTopic(raw)
            }
        }
    }

    private fun parseMarkdownTopic(raw: String): MarkdownTopic? {
        val (frontMatterLines, body) = splitFrontMatter(raw) ?: return null
        val frontMatter = parseFrontMatter(frontMatterLines) ?: return null
        val sections = parseSections(body)
        val topic = WikiTopic(
            id = frontMatter.id,
            title = frontMatter.title,
            summary = frontMatter.summary,
            sections = sections,
            keywords = frontMatter.keywords,
            relatedTopicIds = frontMatter.related,
            glossaryRefIds = frontMatter.glossaryRefs
        )
        return MarkdownTopic(
            topic = topic,
            categoryId = frontMatter.categoryId,
            categoryTitle = frontMatter.categoryTitle,
            categoryDescription = frontMatter.categoryDescription,
            glossaryRefs = frontMatter.glossaryRefs
        )
    }

    private fun splitFrontMatter(raw: String): Pair<List<String>, String>? {
        return FrontMatterParser.split(raw)
    }

    private fun parseFrontMatter(lines: List<String>): FrontMatter? {
        val map = FrontMatterParser.parseKeyValue(lines)
        val id = map["id"]?.takeIf { it.isNotBlank() } ?: return null
        val title = map["title"]?.takeIf { it.isNotBlank() } ?: return null
        val summary = map["summary"]?.takeIf { it.isNotBlank() } ?: ""
        val categoryId = map["category_id"]?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY_ID
        val related = parseList(map["related"])
        val keywords = parseList(map["keywords"])
        val glossaryRefs = parseList(map["glossary_refs"])
        val categoryTitle = map["category_title"]
        val categoryDescription = map["category_description"]
        return FrontMatter(
            id = id,
            title = title,
            summary = summary,
            categoryId = categoryId,
            categoryTitle = categoryTitle,
            categoryDescription = categoryDescription,
            related = related,
            keywords = keywords,
            glossaryRefs = glossaryRefs
        )
    }

    private fun parseList(raw: String?): List<String> {
        return FrontMatterParser.parseList(raw)
    }

    private fun parseSections(body: String): List<WikiSection> {
        if (body.isBlank()) {
            return emptyList()
        }
        val lines = body.lines()
        val sections = mutableListOf<WikiSection>()
        var currentTitle: String? = null
        val buffer = mutableListOf<String>()

        fun flushSection() {
            val paragraphs = buildParagraphs(buffer)
            if (paragraphs.isNotEmpty()) {
                sections += WikiSection(
                    title = currentTitle,
                    paragraphs = paragraphs
                )
            }
            buffer.clear()
        }

        lines.forEach { line ->
            when {
                line.startsWith("## ") -> {
                    if (buffer.isNotEmpty()) {
                        flushSection()
                    }
                    currentTitle = line.removePrefix("## ").trim()
                }
                else -> buffer += line
            }
        }
        if (buffer.isNotEmpty() || sections.isEmpty()) {
            flushSection()
        }
        return sections
    }

    private fun buildParagraphs(lines: List<String>): List<String> {
        val paragraphs = mutableListOf<String>()
        val builder = StringBuilder()
        lines.forEach { line ->
            if (line.isBlank()) {
                if (builder.isNotEmpty()) {
                    paragraphs += builder.toString().trim()
                    builder.clear()
                }
            } else {
                builder.append(line).append('\n')
            }
        }
        if (builder.isNotEmpty()) {
            paragraphs += builder.toString().trim()
        }
        return paragraphs.map(::formatParagraph)
    }

    private fun formatParagraph(text: String): String {
        if (text.isBlank()) return text
        val lines = text.lines()
        val normalized = lines.map { rawLine ->
            val trimmed = rawLine.trimStart()
            val formatted = when {
                trimmed.startsWith("- [ ] ") -> "☐ ${trimmed.removePrefix("- [ ] ").trimStart()}"
                trimmed.startsWith("- [x] ", ignoreCase = true) -> "☑ ${trimmed.removePrefix("- [x] ").trimStart()}"
                trimmed.startsWith("- ") -> "• ${trimmed.removePrefix("- ").trimStart()}"
                trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ").trimStart()}"
                NUMERIC_BULLET_REGEX.matches(trimmed) -> {
                    val value = trimmed.replaceFirst(NUMERIC_PREFIX_REGEX, "")
                    "• ${value.trimStart()}"
                }
                else -> rawLine
            }
            stripMarkdownFormatting(formatted)
        }
        return normalized.joinToString("\n").trimEnd()
    }

    private fun stripMarkdownFormatting(value: String): String {
        return MarkdownFormattingSanitizer.stripInlineMarkdown(value)
    }

    private data class FrontMatter(
        val id: String,
        val title: String,
        val summary: String,
        val categoryId: String,
        val categoryTitle: String?,
        val categoryDescription: String?,
        val related: List<String>,
        val keywords: List<String>,
        val glossaryRefs: List<String>
    )

    data class MarkdownTopic(
        val topic: WikiTopic,
        val categoryId: String,
        val categoryTitle: String?,
        val categoryDescription: String?,
        val glossaryRefs: List<String>
    )

    companion object {
        private const val WIKI_ROOT = "wiki"
        private const val DEFAULT_CATEGORY_ID = "general"
        private val NUMERIC_BULLET_REGEX = Regex("^\\d+\\.\\s+.*")
        private val NUMERIC_PREFIX_REGEX = Regex("^\\d+\\.\\s+")
    }
}
