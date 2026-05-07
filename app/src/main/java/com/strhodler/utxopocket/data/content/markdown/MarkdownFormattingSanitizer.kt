package com.strhodler.utxopocket.data.content.markdown

internal object MarkdownFormattingSanitizer {

    fun stripInlineMarkdown(value: String): String {
        var result = value
        result = BOLD_ASTERISK_REGEX.replace(result) { match -> match.groupValues[1] }
        result = BOLD_UNDERSCORE_REGEX.replace(result) { match -> match.groupValues[1] }
        result = ITALIC_ASTERISK_REGEX.replace(result) { match -> match.groupValues[1] }
        result = ITALIC_UNDERSCORE_REGEX.replace(result) { match -> match.groupValues[1] }
        result = CODE_REGEX.replace(result) { match -> match.groupValues[1] }
        result = LINK_REGEX.replace(result) { match -> match.groupValues[1] }
        return result
    }

    private val BOLD_ASTERISK_REGEX = Regex("\\*\\*(.*?)\\*\\*")
    private val BOLD_UNDERSCORE_REGEX = Regex("__(.*?)__")
    private val ITALIC_ASTERISK_REGEX = Regex("(?<!\\*)\\*(?!\\*)([^*]+?)\\*(?!\\*)")
    private val ITALIC_UNDERSCORE_REGEX = Regex("(?<!_)_(?!_)([^_]+?)_(?!_)")
    private val CODE_REGEX = Regex("`([^`]+)`")
    private val LINK_REGEX = Regex("\\[(.*?)]\\((.*?)\\)")
}
