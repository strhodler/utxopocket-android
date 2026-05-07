package com.strhodler.utxopocket.domain.model

data class GlossaryEntry(
    val id: String,
    val term: String,
    val shortDescription: String,
    val definition: List<String>,
    val aliases: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)
