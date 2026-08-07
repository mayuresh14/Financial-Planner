package com.financeplanner.app.domain.model

data class GoalBasedSipInput(
    val targetAmount: Double,
    val durationYears: Int,
    val expectedReturnPercent: Double,
    val inflationPercent: Double? = null
) {
    init {
        require(targetAmount > 0) { "Target amount must be positive" }
        require(durationYears > 0) { "Duration must be at least 1 year" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

data class GoalBasedSipResult(
    val requiredMonthlySip: Double,
    val inflationAdjustedTarget: Double? = null, // target inflated to future value, if inflation supplied
    val sipForInflationAdjustedTarget: Double? = null // SIP needed to hit the inflated target, info only
)
