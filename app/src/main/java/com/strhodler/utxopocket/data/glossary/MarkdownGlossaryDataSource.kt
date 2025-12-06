package com.strhodler.utxopocket.data.glossary

import android.content.Context
import com.strhodler.utxopocket.presentation.glossary.GlossaryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkdownGlossaryDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun loadEntries(): List<GlossaryEntry> {
        val assetManager = context.assets
        val markdownFiles = collectMarkdownFiles(assetManager.list(GLOSSARY_ROOT) ?: emptyArray())
        return markdownFiles.mapNotNull { relativePath ->
            val fullPath = if (relativePath.isEmpty()) GLOSSARY_ROOT else "$GLOSSARY_ROOT/$relativePath"
            runCatching {
                assetManager.open(fullPath).bufferedReader().use(BufferedReader::readText)
            }.getOrNull()?.let { raw ->
                parseMarkdownEntry(raw)
            }
        }
    }

    private fun collectMarkdownFiles(entries: Array<String>, prefix: String = ""): List<String> {
        val assetManager = context.assets
        val files = mutableListOf<String>()
        entries.forEach { entry ->
            val relativePath = if (prefix.isEmpty()) entry else "$prefix/$entry"
            val assetPath = if (prefix.isEmpty()) "$GLOSSARY_ROOT/$entry" else "$GLOSSARY_ROOT/$relativePath"
            val children = runCatching { assetManager.list(assetPath) }.getOrNull()
            if (children != null && children.isNotEmpty()) {
                files += collectMarkdownFiles(children, relativePath)
            } else if (entry.endsWith(".md")) {
                files += relativePath
            }
        }
        return files
    }

    private fun parseMarkdownEntry(raw: String): GlossaryEntry? {
        val (frontMatterLines, body) = splitFrontMatter(raw) ?: return null
        val fm = parseFrontMatter(frontMatterLines) ?: return null
        val definition = buildParagraphs(body)
        return GlossaryEntry(
            id = fm.id,
            term = fm.title,
            shortDescription = fm.summary,
            definition = definition,
            aliases = fm.aliases,
            keywords = fm.keywords
        )
    }

    private fun splitFrontMatter(raw: String): Pair<List<String>, String>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(FRONT_MATTER_DELIMITER)) return null
        val lines = trimmed.lines()
        val frontMatterLines = mutableListOf<String>()
        var index = 1
        while (index < lines.size) {
            val line = lines[index]
            if (line.trim() == FRONT_MATTER_DELIMITER) break
            frontMatterLines += line
            index++
        }
        if (index >= lines.size) return null
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
        val aliases = parseList(map["aliases"]) 
        val keywords = parseList(map["keywords"]) 
        return FrontMatter(
            id = id,
            title = title,
            summary = summary,
            aliases = aliases,
            keywords = keywords
        )
    }

    private fun parseList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.removePrefix("[").removeSuffix("]")
        return trimmed.split(",")
            .mapNotNull { item -> item.trim().takeIf { it.isNotBlank() } }
    }

    private fun buildParagraphs(body: String): List<String> {
        if (body.isBlank()) return emptyList()
        val lines = body.lines()
        val paragraphs = mutableListOf<String>()
        val builder = StringBuilder()
        fun flush() {
            if (builder.isNotEmpty()) {
                paragraphs += builder.toString().trim()
                builder.clear()
            }
        }
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                flush()
            } else {
                builder.append(stripMarkdownFormatting(line)).append('\n')
            }
        }
        flush()
        return paragraphs
    }

    private fun stripMarkdownFormatting(value: String): String {
        var result = value
        result = BOLD_ASTERISK_REGEX.replace(result) { match -> match.groupValues[1] }
        result = BOLD_UNDERSCORE_REGEX.replace(result) { match -> match.groupValues[1] }
        result = ITALIC_ASTERISK_REGEX.replace(result) { match -> match.groupValues[1] }
        result = ITALIC_UNDERSCORE_REGEX.replace(result) { match -> match.groupValues[1] }
        result = CODE_REGEX.replace(result) { match -> match.groupValues[1] }
        result = LINK_REGEX.replace(result) { match -> match.groupValues[1] }
        return result
    }

    private data class FrontMatter(
        val id: String,
        val title: String,
        val summary: String,
        val aliases: List<String>,
        val keywords: List<String>
    )

    companion object {
        private const val GLOSSARY_ROOT = "glossary"
        private const val FRONT_MATTER_DELIMITER = "---"
        private val BOLD_ASTERISK_REGEX = Regex("\\*\\*(.*?)\\*\\*")
        private val BOLD_UNDERSCORE_REGEX = Regex("__(.*?)__")
        private val ITALIC_ASTERISK_REGEX = Regex("(?<!\\*)\\*(?!\\*)([^*]+?)\\*(?!\\*)")
        private val ITALIC_UNDERSCORE_REGEX = Regex("(?<!_)_(?!_)([^_]+?)_(?!_)")
        private val CODE_REGEX = Regex("`([^`]+)`")
        private val LINK_REGEX = Regex("\\[(.*?)]\\((.*?)\\)")
    }
}
