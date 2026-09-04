package com.financeplanner.app.domain.model

/**
 * Which calculator a saved investment record originated from — real
 * financial instruments you actually hold (belong to a bank/institution).
 * SIP/LUMPSUM/GOAL_BASED_SIP appear here too, since those can be either a
 * real recurring/one-time investment OR a "what-if" scenario — the user
 * picks which when saving (see [com.financeplanner.app.domain.model.SaveTarget]).
 * The scenario-only side of the same three calculators lives in
 * [SavedCalculationType] instead.
 */
enum class SavedInvestmentType {
    FD, RD, PPF, EPF, SSY, NPS, SIP, LUMPSUM, GOAL_BASED_SIP
}

/**
 * A user-named snapshot of a calculator's inputs, so the same calculator can
 * be reused for multiple real-world instances (e.g. two RDs at different
 * banks for different purposes) and told apart later. [detailsJson] holds
 * the type-specific input fields as a JSON object — kept as one flexible
 * column rather than one table per type, since every type's fields are only
 * ever read back into that same calculator's input form.
 */
data class SavedInvestment(
    val id: Long = 0,
    val type: SavedInvestmentType,
    val customName: String,
    val institutionName: String?,
    val notes: String?,
    val detailsJson: String,
    val lastComputedValue: Double?,
    val createdAt: Long
)
