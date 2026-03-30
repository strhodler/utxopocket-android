package com.strhodler.utxopocket.domain.privacy

data class PrivacySummaryEntry(
    val severity: PrivacySeverity,
    val count: Int,
    val findingIds: List<String>
)

data class PrivacySummary(
    val entries: List<PrivacySummaryEntry>
) {
    val totalFindings: Int = entries.sumOf(PrivacySummaryEntry::count)

    companion object {
        val Empty: PrivacySummary = PrivacySummary(entries = emptyList())

        fun from(findings: List<PrivacyFinding>): PrivacySummary {
            if (findings.isEmpty()) return Empty

            val entries = findings
                .groupBy(PrivacyFinding::severity)
                .map { (severity, groupedFindings) ->
                    PrivacySummaryEntry(
                        severity = severity,
                        count = groupedFindings.size,
                        findingIds = groupedFindings.map(PrivacyFinding::id).sorted()
                    )
                }
                .sortedWith(
                    compareBy<PrivacySummaryEntry>(::severityOrder)
                        .thenByDescending(PrivacySummaryEntry::count)
                        .thenBy { it.findingIds.firstOrNull().orEmpty() }
                )

            return PrivacySummary(entries = entries)
        }

        private fun severityOrder(entry: PrivacySummaryEntry): Int = when (entry.severity) {
            PrivacySeverity.Critical -> 0
            PrivacySeverity.Warning -> 1
            PrivacySeverity.Caution -> 2
            PrivacySeverity.Info -> 3
            PrivacySeverity.Positive -> 4
        }
    }
}
