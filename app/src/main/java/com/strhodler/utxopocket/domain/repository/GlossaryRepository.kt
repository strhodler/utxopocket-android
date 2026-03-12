package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.GlossaryEntry

interface GlossaryRepository {
    fun entries(): List<GlossaryEntry>
    fun entryById(id: String): GlossaryEntry?
}
