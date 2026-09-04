package com.financeplanner.app.domain.model

data class NpsInput(
    val monthlyContribution: Double,
    val currentAge: Int,
    val expectedReturnPercent: Double,
    val annuityPercent: Double = 40.0, // % of corpus used to buy annuity; configurable, not hardcoded — PFRDA rules shift over time
    val inflationPercent: Double? = null,
    /** Balance already in the account before this projection starts — for someone tracking
     * an account they opened before using this app, not one starting from zero. */
    val existingBalance: Double = 0.0
) {
    init {
        require(monthlyContribution > 0) { "Monthly contribution must be positive" }
        require(currentAge in 18..59) { "NPS is open to subscribers aged 18-59" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        require(annuityPercent in 40.0..100.0) { "Minimum 40% of NPS corpus must go towards annuity per current rules" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
        require(existingBalance >= 0) { "Existing balance cannot be negative" }
    }
}

data class NpsResult(
    val corpusAtSixty: Double,
    val lumpsumWithdrawal: Double,
    val annuityCorpus: Double,
    val estimatedMonthlyPension: Double,
    val inflationAdjustedValue: Double? = null
)
