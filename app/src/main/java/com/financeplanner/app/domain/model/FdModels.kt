package com.financeplanner.app.domain.model

data class FdInput(
    val principal: Double,
    val annualRatePercent: Double,
    val tenureMonths: Int,
    val inflationPercent: Double? = null
) {
    init {
        require(principal > 0) { "Principal must be positive" }
        require(annualRatePercent >= 0) { "Interest rate cannot be negative" }
        require(tenureMonths > 0) { "Tenure must be at least 1 month" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

data class FdResult(
    val maturityValue: Double,
    val totalInterestEarned: Double,
    val inflationAdjustedValue: Double? = null
)
