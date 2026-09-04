package com.financeplanner.app.domain.model

/**
 * Which calculator a saved calculation originated from — "what-if" scenario
 * snapshots, not real accounts. Contrast with [SavedInvestmentType], which
 * covers real instruments (FD/RD/PPF/EPF/SSY/NPS) with actual dates.
 */
enum class SavedCalculationType {
    SIP, LUMPSUM, GOAL_BASED_SIP
}

/**
 * A user-named snapshot of a calculator's inputs, kept for later reference
 * rather than to track a real account — no institution/bank, no maturity
 * date, just "what did I calculate and what did it say."
 */
data class SavedCalculation(
    val id: Long = 0,
    val type: SavedCalculationType,
    val customName: String,
    val notes: String?,
    val detailsJson: String,
    val lastComputedValue: Double?,
    val createdAt: Long
)
