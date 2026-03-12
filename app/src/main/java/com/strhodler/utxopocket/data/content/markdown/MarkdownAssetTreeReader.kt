package com.strhodler.utxopocket.data.content.markdown

import android.content.res.AssetManager

internal object MarkdownAssetTreeReader {

    fun collectMarkdownFiles(assetManager: AssetManager, root: String): List<String> {
        val entries = assetManager.list(root) ?: emptyArray()
        return collectMarkdownFiles(assetManager, root, entries)
    }

    private fun collectMarkdownFiles(
        assetManager: AssetManager,
        root: String,
        entries: Array<String>,
        prefix: String = ""
    ): List<String> {
        val files = mutableListOf<String>()
        entries.forEach { entry ->
            val relativePath = if (prefix.isEmpty()) entry else "$prefix/$entry"
            val assetPath = if (prefix.isEmpty()) "$root/$entry" else "$root/$relativePath"
            val children = runCatching { assetManager.list(assetPath) }.getOrNull()
            if (children != null && children.isNotEmpty()) {
                files += collectMarkdownFiles(assetManager, root, children, relativePath)
            } else if (entry.endsWith(".md")) {
                files += relativePath
            }
        }
        return files
    }
}
