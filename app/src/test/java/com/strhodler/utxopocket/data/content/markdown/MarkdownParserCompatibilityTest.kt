package com.strhodler.utxopocket.data.content.markdown

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.glossary.MarkdownGlossaryDataSource
import com.strhodler.utxopocket.data.wiki.MarkdownWikiDataSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkdownParserCompatibilityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun wikiFormatParagraphConvertsListStylesAndStripsInlineMarkdown() {
        val dataSource = MarkdownWikiDataSource(context)
        val input = """
            - [ ] **Task** with `code`
            - [x] _Done_ item
            - [ ] [Link](https://example.com)
            - bullet line
            * star bullet
            1. numbered item
        """.trimIndent()

        val formatted = invokeWikiFormatParagraph(dataSource, input)

        val expected = listOf(
            "☐ Task with code",
            "☑ Done item",
            "☐ Link",
            "• bullet line",
            "• star bullet",
            "• numbered item"
        ).joinToString("\n")
        assertEquals(expected, formatted)
    }

    @Test
    fun wikiFrontmatterParsingKeepsListFieldCompatibility() {
        val dataSource = MarkdownWikiDataSource(context)
        val frontMatter = invokeWikiParseFrontMatter(
            dataSource,
            listOf(
                "id: sample-topic",
                "title: Sample Topic",
                "summary: Sample summary",
                "category_id: sample-category",
                "related: [one, two]",
                "keywords: [alpha, beta]",
                "glossary_refs: [utxo, descriptor]"
            )
        )

        assertNotNull(frontMatter)
        assertEquals(listOf("one", "two"), readListField(frontMatter, "related"))
        assertEquals(listOf("alpha", "beta"), readListField(frontMatter, "keywords"))
        assertEquals(listOf("utxo", "descriptor"), readListField(frontMatter, "glossaryRefs"))
    }

    @Test
    fun descriptorCompatibilityMarkdownFileParsesIntoExpectedTopicShape() {
        val dataSource = MarkdownWikiDataSource(context)
        val raw = readProjectFile("docs/wiki/descriptor-compatibility.md")

        val markdownTopic = invokeWikiParseMarkdownTopic(dataSource, raw)
        val topic = markdownTopic.topic

        assertEquals("descriptor-compatibility", topic.id)
        assertTrue(topic.relatedTopicIds.contains("descriptors-advanced"))
        assertTrue(topic.glossaryRefIds.contains("descriptor"))
        assertTrue(topic.sections.any { section -> section.title == "Combined external + change exports" })
    }

    @Test
    fun blockExplorerPrivacyMarkdownFileParsesIntoExpectedTopicShape() {
        val dataSource = MarkdownWikiDataSource(context)
        val raw = readProjectFile("docs/wiki/block-explorer-privacy.md")

        val markdownTopic = invokeWikiParseMarkdownTopic(dataSource, raw)
        val topic = markdownTopic.topic

        assertEquals("block-explorer-privacy", topic.id)
        assertTrue(topic.relatedTopicIds.contains("privacy-threat-models"))
        assertTrue(topic.relatedTopicIds.contains("node-connectivity"))
    }

    @Test
    fun privacyThreatModelsMarkdownFileParsesIntoExpectedTopicShape() {
        val dataSource = MarkdownWikiDataSource(context)
        val raw = readProjectFile("docs/wiki/privacy-threat-models.md")

        val markdownTopic = invokeWikiParseMarkdownTopic(dataSource, raw)
        val topic = markdownTopic.topic

        assertEquals("privacy-threat-models", topic.id)
        assertEquals("Threat Models", topic.title)
        assertTrue(topic.glossaryRefIds.contains("tor"))
    }

    @Test
    fun glossaryStripMarkdownFormattingRemainsStable() {
        val dataSource = MarkdownGlossaryDataSource(context)
        val input = "Use **bold**, _italic_, `code`, and [link](https://example.com)."

        val formatted = invokeGlossaryStripFormatting(dataSource, input)

        assertEquals("Use bold, italic, code, and link.", formatted)
    }

    @Test
    fun glossaryFrontmatterParsingKeepsAliasesAndKeywordsCompatibility() {
        val dataSource = MarkdownGlossaryDataSource(context)
        val frontMatter = invokeGlossaryParseFrontMatter(
            dataSource,
            listOf(
                "id: sample-entry",
                "title: Sample Entry",
                "summary: Sample definition",
                "aliases: [abbr, synonym]",
                "keywords: [privacy, wallet]"
            )
        )

        assertNotNull(frontMatter)
        assertEquals(listOf("abbr", "synonym"), readListField(frontMatter, "aliases"))
        assertEquals(listOf("privacy", "wallet"), readListField(frontMatter, "keywords"))
    }

    @Test
    fun psbtGlossaryMarkdownFileParsesIntoExpectedEntryShape() {
        val dataSource = MarkdownGlossaryDataSource(context)
        val raw = readProjectFile("docs/glossary/psbt.md")

        val entry = invokeGlossaryParseMarkdownEntry(dataSource, raw)

        assertEquals("psbt", entry.id)
        assertEquals("PSBT (Partially Signed Bitcoin Transaction)", entry.term)
        assertTrue(entry.keywords.contains("psbt"))
    }

    private fun invokeWikiFormatParagraph(dataSource: MarkdownWikiDataSource, value: String): String {
        val method = MarkdownWikiDataSource::class.java.getDeclaredMethod("formatParagraph", String::class.java)
        method.isAccessible = true
        return method.invoke(dataSource, value) as String
    }

    private fun invokeWikiParseFrontMatter(dataSource: MarkdownWikiDataSource, lines: List<String>): Any? {
        val method = MarkdownWikiDataSource::class.java.getDeclaredMethod("parseFrontMatter", List::class.java)
        method.isAccessible = true
        return method.invoke(dataSource, lines)
    }

    private fun invokeWikiParseMarkdownTopic(
        dataSource: MarkdownWikiDataSource,
        raw: String
    ): MarkdownWikiDataSource.MarkdownTopic {
        val method = MarkdownWikiDataSource::class.java.getDeclaredMethod("parseMarkdownTopic", String::class.java)
        method.isAccessible = true
        return method.invoke(dataSource, raw) as MarkdownWikiDataSource.MarkdownTopic
    }

    private fun invokeGlossaryStripFormatting(dataSource: MarkdownGlossaryDataSource, value: String): String {
        val method = MarkdownGlossaryDataSource::class.java.getDeclaredMethod("stripMarkdownFormatting", String::class.java)
        method.isAccessible = true
        return method.invoke(dataSource, value) as String
    }

    private fun invokeGlossaryParseFrontMatter(dataSource: MarkdownGlossaryDataSource, lines: List<String>): Any? {
        val method = MarkdownGlossaryDataSource::class.java.getDeclaredMethod("parseFrontMatter", List::class.java)
        method.isAccessible = true
        return method.invoke(dataSource, lines)
    }

    private fun invokeGlossaryParseMarkdownEntry(
        dataSource: MarkdownGlossaryDataSource,
        raw: String
    ): com.strhodler.utxopocket.presentation.glossary.GlossaryEntry {
        val method = MarkdownGlossaryDataSource::class.java.getDeclaredMethod("parseMarkdownEntry", String::class.java)
        method.isAccessible = true
        return method.invoke(dataSource, raw) as com.strhodler.utxopocket.presentation.glossary.GlossaryEntry
    }

    private fun readProjectFile(relativePath: String): String {
        val startDir = System.getProperty("user.dir") ?: "."
        var current = File(startDir)
        while (true) {
            val candidate = File(current, relativePath)
            if (candidate.exists()) {
                return candidate.readText()
            }
            val parent = current.parentFile ?: break
            current = parent
        }
        throw IllegalStateException("Could not locate project file: $relativePath from $startDir")
    }

    @Suppress("UNCHECKED_CAST")
    private fun readListField(instance: Any, fieldName: String): List<String> {
        val field = instance.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(instance) as List<String>
    }
}
