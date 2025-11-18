package com.strhodler.utxopocket.data.wiki

import android.content.Context
import com.strhodler.utxopocket.presentation.wiki.WikiSection
import com.strhodler.utxopocket.presentation.wiki.WikiTopic
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkdownWikiDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun loadTopics(): List<MarkdownTopic> {
        val assetManager = context.assets
        val markdownFiles = collectMarkdownFiles(assetManager.list(WIKI_ROOT) ?: emptyArray())
        return markdownFiles.mapNotNull { relativePath ->
            val fullPath = if (relativePath.isEmpty()) WIKI_ROOT else "$WIKI_ROOT/$relativePath"
            runCatching {
                assetManager.open(fullPath).bufferedReader().use(BufferedReader::readText)
            }.getOrNull()?.let { raw ->
                parseMarkdownTopic(raw)
            }
        }
    }

    private fun collectMarkdownFiles(entries: Array<String>, prefix: String = ""): List<String> {
        val assetManager = context.assets
        val files = mutableListOf<String>()
        entries.forEach { entry ->
            val relativePath = if (prefix.isEmpty()) entry else "$prefix/$entry"
            val assetPath = if (prefix.isEmpty()) "$WIKI_ROOT/$entry" else "$WIKI_ROOT/$relativePath"
            val children = runCatching { assetManager.list(assetPath) }.getOrNull()
            if (children != null && children.isNotEmpty()) {
                files += collectMarkdownFiles(children, relativePath)
            } else if (entry.endsWith(".md")) {
                files += relativePath
            }
        }
        return files
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
        val trimmed = raw.trim()
        if (!trimmed.startsWith(FRONT_MATTER_DELIMITER)) {
            return null
        }
        val lines = trimmed.lines()
        val frontMatterLines = mutableListOf<String>()
        var index = 1
        while (index < lines.size) {
            val line = lines[index]
            if (line.trim() == FRONT_MATTER_DELIMITER) {
                break
            }
            frontMatterLines += line
            index++
        }
        if (index >= lines.size) {
            return null
        }
        val body = lines.drop(index + 1).joinToString("\n").trim()
        return frontMatterLines to body
    }

    private fun parseFrontMatter(lines: List<String>): FrontMatter? {
        val map = mutableMapOf<String, String>()
        lines.forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                map[parts[0].trim().lowercase()] = parts[1].trim()
            }
        }
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
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.removePrefix("[").removeSuffix("]")
        return trimmed.split(",")
            .mapNotNull { item -> item.trim().takeIf { it.isNotBlank() } }
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
        if (lines.isEmpty()) return emptyList()
        val paragraphs = mutableListOf<String>()
        val buffer = StringBuilder()
        var insideCodeBlock = false

        fun flush() {
            if (buffer.isNotEmpty()) {
                paragraphs += buffer.toString().trimEnd('\n')
                buffer.clear()
            }
        }

        lines.forEach { line ->
            val trimmed = line.trim()
            val isFence = trimmed.startsWith("```")
            when {
                isFence -> {
                    buffer.append(line).append('\n')
                    if (insideCodeBlock) {
                        insideCodeBlock = false
                        flush()
                    } else {
                        insideCodeBlock = true
                    }
                }
                insideCodeBlock -> buffer.append(line).append('\n')
                line.isBlank() -> flush()
                else -> buffer.append(line).append('\n')
            }
        }
        flush()
        return paragraphs
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
        private const val FRONT_MATTER_DELIMITER = "---"
        private const val DEFAULT_CATEGORY_ID = "general"
    }
}
