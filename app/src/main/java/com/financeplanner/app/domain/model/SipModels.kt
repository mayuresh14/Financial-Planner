package com.financeplanner.app.domain.model

/**
 * Input for the SIP calculator. [stepUpPercent] and [inflationPercent] are
 * optional per the requirements doc — SIP calculator supports an
 * inflation-adjusted view and an optional annual step-up.
 */
data class SipInput(
    val monthlyAmount: Double,
    val expectedReturnPercent: Double,
    val durationYears: Int,
    val stepUpPercent: Double? = null,
    val inflationPercent: Double? = null
) {
    init {
        require(monthlyAmount > 0) { "Monthly amount must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        require(durationYears > 0) { "Duration must be at least 1 year" }
        stepUpPercent?.let { require(it >= 0) { "Step-up percent cannot be negative" } }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

/**
 * Result of a SIP calculation. [inflationAdjustedValue] is null when the
 * user didn't supply an inflation rate.
 */
data class SipResult(
    val maturityValue: Double,
    val totalInvested: Double,
    val wealthGained: Double,
    val inflationAdjustedValue: Double? = null
)
