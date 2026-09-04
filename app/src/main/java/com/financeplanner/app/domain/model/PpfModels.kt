package com.financeplanner.app.domain.model

data class PpfInput(
    val yearlyContribution: Double,
    val durationYears: Int,
    val interestRatePercent: Double = 7.1, // current PPF rate as of last known notification; user-editable
    val inflationPercent: Double? = null,
    /** Balance already in the account before this projection starts — for someone tracking
     * an account they opened before using this app, not one starting from zero. */
    val existingBalance: Double = 0.0
) {
    init {
        require(yearlyContribution > 0) { "Yearly contribution must be positive" }
        require(yearlyContribution <= 150_000) { "PPF yearly contribution cannot exceed ₹1,50,000" }
        require(durationYears >= 15) { "PPF has a minimum 15-year lock-in" }
        require(interestRatePercent >= 0) { "Interest rate cannot be negative" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
        require(existingBalance >= 0) { "Existing balance cannot be negative" }
    }
}

data class PpfResult(
    val maturityValue: Double,
    val totalContributed: Double,
    val interestEarned: Double,
    val inflationAdjustedValue: Double? = null
)
