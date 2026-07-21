package com.financeplanner.app.domain.model

data class SmokeInput(
    val cigarettesPerDay: Int,
    val costPerCigarette: Double,
    val years: Int,
    val expectedReturnPercent: Double,
    val inflationPercent: Double? = null
) {
    init {
        require(cigarettesPerDay > 0) { "Cigarettes per day must be positive" }
        require(costPerCigarette > 0) { "Cost per cigarette must be positive" }
        require(years > 0) { "Years must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

data class SmokeResult(
    val monthlySpend: Double,
    val totalMoneySpent: Double,
    val investedValue: Double,
    val inflationAdjustedValue: Double? = null
)
