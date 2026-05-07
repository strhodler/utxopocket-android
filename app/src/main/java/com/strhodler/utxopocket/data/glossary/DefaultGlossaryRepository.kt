package com.strhodler.utxopocket.data.glossary

import com.strhodler.utxopocket.domain.model.GlossaryEntry
import com.strhodler.utxopocket.domain.repository.GlossaryRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGlossaryRepository private constructor(
    markdownEntries: List<GlossaryEntry>
) : GlossaryRepository {

    @Inject
    constructor(markdownGlossaryDataSource: MarkdownGlossaryDataSource) : this(
        markdownEntries = markdownGlossaryDataSource.loadEntries()
    )

    private val catalog: Catalog

    init {
        val entries = markdownEntries
            .sortedBy { it.term.lowercase(Locale.ROOT) }
        catalog = Catalog(
            entries = entries,
            entriesById = entries.associateBy { it.id }
        )
    }

    override fun entries(): List<GlossaryEntry> = catalog.entries

    override fun entryById(id: String): GlossaryEntry? = catalog.entriesById[id]

    private data class Catalog(
        val entries: List<GlossaryEntry>,
        val entriesById: Map<String, GlossaryEntry>
    )

    companion object {
        internal fun fromMarkdownEntries(markdownEntries: List<GlossaryEntry>): DefaultGlossaryRepository =
            DefaultGlossaryRepository(markdownEntries)
    }
}
