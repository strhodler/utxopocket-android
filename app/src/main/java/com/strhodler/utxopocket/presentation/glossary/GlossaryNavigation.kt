package com.strhodler.utxopocket.presentation.glossary

object GlossaryNavigation {
    const val ListRoute: String = "glossary/list"
    const val SearchRoute: String = "glossary/search"
    const val EntryIdArg: String = "entryId"
    const val DetailRoute: String = "glossary/detail/{$EntryIdArg}"

    fun detailRoute(entryId: String): String = "glossary/detail/$entryId"
}
