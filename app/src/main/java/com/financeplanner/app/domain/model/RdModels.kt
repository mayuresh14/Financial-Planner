package com.financeplanner.app.domain.model

data class RdInput(
    val monthlyDeposit: Double,
    val annualRatePercent: Double,
    val tenureMonths: Int,
    val inflationPercent: Double? = null
) {
    init {
        require(monthlyDeposit > 0) { "Monthly deposit must be positive" }
        require(annualRatePercent >= 0) { "Interest rate cannot be negative" }
        require(tenureMonths > 0) { "Tenure must be at least 1 month" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

data class RdResult(
    val maturityValue: Double,
    val totalDeposited: Double,
    val interestEarned: Double,
    val inflationAdjustedValue: Double? = null
)
