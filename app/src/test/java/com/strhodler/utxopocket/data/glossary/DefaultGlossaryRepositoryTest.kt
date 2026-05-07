package com.strhodler.utxopocket.data.glossary

import com.strhodler.utxopocket.domain.model.GlossaryEntry
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultGlossaryRepositoryTest {

    @Test
    fun entryByIdResolvesCriticalMarkdownEntries() {
        val repository = createRepository()

        listOf(
            "utxo",
            "descriptor",
            "seed-phrase",
            "electrum-server",
            "tor",
            "coin-control"
        ).forEach { id ->
            assertNotNull(repository.entryById(id), "Expected glossary entry for id=$id")
        }
    }

    @Test
    fun repositoryCatalogMatchesProvidedMarkdownEntriesExactly() {
        val repository = createRepository()
        val markdownEntries = fixtureEntries.sortedBy { it.term.lowercase(Locale.ROOT) }

        assertEquals(markdownEntries, repository.entries())
        markdownEntries.forEach { markdownEntry ->
            assertEquals(markdownEntry, repository.entryById(markdownEntry.id))
        }
    }

    @Test
    fun entriesRemainSortedByTermLowercase() {
        val repository = createRepository()

        val terms = repository.entries().map { it.term }
        assertEquals(terms.sortedBy { it.lowercase(Locale.ROOT) }, terms)
    }

    @Test
    fun repositoryDoesNotUseLegacyFallbackWhenMarkdownEntryIsMissing() {
        val markdownOnlyId = "markdown-only"
        val repository = DefaultGlossaryRepository.fromMarkdownEntries(
            listOf(
                entry(id = markdownOnlyId, term = "Markdown Only")
            )
        )

        assertNotNull(repository.entryById(markdownOnlyId))
        assertEquals(null, repository.entryById("utxo"))
    }

    private fun createRepository(): DefaultGlossaryRepository =
        DefaultGlossaryRepository.fromMarkdownEntries(fixtureEntries)

    private fun entry(id: String, term: String): GlossaryEntry = GlossaryEntry(
        id = id,
        term = term,
        shortDescription = "$term summary",
        definition = listOf("$term definition"),
        aliases = emptyList(),
        keywords = listOf(term.lowercase(Locale.ROOT))
    )

    private val fixtureEntries: List<GlossaryEntry> = listOf(
        entry(id = "tor", term = "Tor"),
        entry(id = "utxo", term = "UTXO"),
        entry(id = "descriptor", term = "Descriptor"),
        entry(id = "seed-phrase", term = "Seed phrase"),
        entry(id = "electrum-server", term = "Electrum server"),
        entry(id = "coin-control", term = "Coin control")
    )
}
