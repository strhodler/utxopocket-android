package com.strhodler.utxopocket.data.content.markdown

internal object FrontMatterParser {

    fun split(raw: String): Pair<List<String>, String>? {
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

    fun parseKeyValue(lines: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        lines.forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                map[parts[0].trim().lowercase()] = parts[1].trim()
            }
        }
        return map
    }

    fun parseList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.removePrefix("[").removeSuffix("]")
        return trimmed.split(",")
            .mapNotNull { item -> item.trim().takeIf { it.isNotBlank() } }
    }

    private const val FRONT_MATTER_DELIMITER = "---"
}
