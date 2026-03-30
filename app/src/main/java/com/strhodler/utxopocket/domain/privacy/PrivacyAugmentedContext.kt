package com.strhodler.utxopocket.domain.privacy

data class PrivacyAugmentedContext(
    val enabled: Boolean = false,
    val facts: Map<String, String> = emptyMap(),
    val relatedKeys: Map<String, Set<String>> = emptyMap()
) {
    companion object {
        val None: PrivacyAugmentedContext = PrivacyAugmentedContext()
    }
}
