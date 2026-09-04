package com.financeplanner.app.domain.model

data class SsyInput(
    val girlAgeAtOpening: Int,
    val yearlyDeposit: Double,
    val interestRatePercent: Double = 8.2, // current SSY rate as of last known notification; user-editable
    val inflationPercent: Double? = null,
    /** Balance already in the account before this projection starts — for someone tracking
     * an account they opened before using this app, not one starting from zero. */
    val existingBalance: Double = 0.0
) {
    init {
        require(girlAgeAtOpening in 0..9) { "SSY accounts can only be opened for a girl child under 10 years old" }
        require(yearlyDeposit > 0) { "Yearly deposit must be positive" }
        require(yearlyDeposit <= 150_000) { "SSY yearly deposit cannot exceed ₹1,50,000" }
        require(interestRatePercent >= 0) { "Interest rate cannot be negative" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
        require(existingBalance >= 0) { "Existing balance cannot be negative" }
    }
}

data class SsyResult(
    val maturityValue: Double,
    val totalDeposited: Double,
    val interestEarned: Double,
    val girlAgeAtMaturity: Int,
    val inflationAdjustedValue: Double? = null
)
