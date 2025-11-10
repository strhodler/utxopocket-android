package com.strhodler.utxopocket.data.glossary

import com.strhodler.utxopocket.presentation.glossary.GlossaryContent
import com.strhodler.utxopocket.presentation.glossary.GlossaryEntry
import javax.inject.Inject
import javax.inject.Singleton

interface GlossaryRepository {
    fun entries(): List<GlossaryEntry>
    fun entryById(id: String): GlossaryEntry?
}

@Singleton
class DefaultGlossaryRepository @Inject constructor(
    markdownGlossaryDataSource: MarkdownGlossaryDataSource
) : GlossaryRepository {

    private val catalog: Catalog

    init {
        val fallback = GlossaryContent.entries
        val builders = fallback.associateBy({ it.id }, { it.copy() }).toMutableMap()

        val markdownEntries = markdownGlossaryDataSource.loadEntries()
        markdownEntries.forEach { entry ->
            builders[entry.id] = entry
        }

        val merged = builders.values
            .sortedBy { it.term.lowercase() }
        val byId = merged.associateBy { it.id }
        catalog = Catalog(entries = merged, entriesById = byId)
    }

    override fun entries(): List<GlossaryEntry> = catalog.entries

    override fun entryById(id: String): GlossaryEntry? = catalog.entriesById[id]

    private data class Catalog(
        val entries: List<GlossaryEntry>,
        val entriesById: Map<String, GlossaryEntry>
    )
}

