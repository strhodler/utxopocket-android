package com.strhodler.utxopocket.presentation.wallets.detail

internal fun combineDescriptorBranches(
    externalDescriptor: String?,
    changeDescriptor: String?
): String? {
    val external = externalDescriptor?.let(::parseDescriptorBranch) ?: return null
    val change = changeDescriptor?.let(::parseDescriptorBranch) ?: return null

    if (external.branch == change.branch) return null
    if (setOf(external.branch, change.branch) != setOf(0, 1)) return null
    if (external.base != change.base) return null
    if (external.closingParens != change.closingParens) return null

    return buildString {
        append(external.base)
        append("/<0;1>/*")
        append(external.closingParens)
    }
}

private data class DescriptorBranch(
    val base: String,
    val branch: Int,
    val closingParens: String
)

private fun parseDescriptorBranch(value: String): DescriptorBranch? {
    val sanitized = value.substringBefore("#").trim()
    if (sanitized.isEmpty()) return null

    val closingParensCount = sanitized.takeLastWhile { it == ')' }.length
    val closingParens = if (closingParensCount > 0) {
        sanitized.takeLast(closingParensCount)
    } else {
        ""
    }
    val core = if (closingParensCount > 0) sanitized.dropLast(closingParensCount) else sanitized

    val branch = when {
        core.endsWith("/0/*") -> 0
        core.endsWith("/1/*") -> 1
        else -> return null
    }
    val base = core.removeSuffix("/$branch/*")

    return DescriptorBranch(
        base = base,
        branch = branch,
        closingParens = closingParens
    )
}
