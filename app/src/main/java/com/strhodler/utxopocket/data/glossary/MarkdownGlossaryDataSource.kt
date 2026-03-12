package com.strhodler.utxopocket.data.glossary

import android.content.Context
import com.strhodler.utxopocket.data.content.markdown.FrontMatterParser
import com.strhodler.utxopocket.data.content.markdown.MarkdownAssetTreeReader
import com.strhodler.utxopocket.data.content.markdown.MarkdownFormattingSanitizer
import com.strhodler.utxopocket.domain.model.GlossaryEntry
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
        val markdownFiles = MarkdownAssetTreeReader.collectMarkdownFiles(assetManager, GLOSSARY_ROOT)
        return markdownFiles.mapNotNull { relativePath ->
            val fullPath = if (relativePath.isEmpty()) GLOSSARY_ROOT else "$GLOSSARY_ROOT/$relativePath"
            runCatching {
                assetManager.open(fullPath).bufferedReader().use(BufferedReader::readText)
            }.getOrNull()?.let { raw ->
                parseMarkdownEntry(raw)
            }
        }
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
        return FrontMatterParser.split(raw)
    }

    private fun parseFrontMatter(lines: List<String>): FrontMatter? {
        val map = FrontMatterParser.parseKeyValue(lines)
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
        return FrontMatterParser.parseList(raw)
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
        return MarkdownFormattingSanitizer.stripInlineMarkdown(value)
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
    }
}
