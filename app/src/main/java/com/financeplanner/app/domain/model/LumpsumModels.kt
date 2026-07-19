package com.financeplanner.app.domain.model

data class LumpsumInput(
    val principal: Double,
    val expectedReturnPercent: Double,
    val durationYears: Int,
    val inflationPercent: Double? = null
) {
    init {
        require(principal > 0) { "Principal must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        require(durationYears > 0) { "Duration must be at least 1 year" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

data class LumpsumResult(
    val maturityValue: Double,
    val principal: Double,
    val wealthGained: Double,
    val inflationAdjustedValue: Double? = null
)
