package com.financeplanner.app.domain.model

data class EatingOutInput(
    val eatingOutMonthlySpend: Double,
    val homeCookingMonthlySpend: Double,
    val years: Int,
    val expectedReturnPercent: Double,
    val inflationPercent: Double? = null
) {
    init {
        require(eatingOutMonthlySpend > 0) { "Eating out spend must be positive" }
        require(homeCookingMonthlySpend >= 0) { "Home cooking spend cannot be negative" }
        require(eatingOutMonthlySpend > homeCookingMonthlySpend) { "Eating out spend must exceed home cooking spend to show savings" }
        require(years > 0) { "Years must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

data class EatingOutResult(
    val monthlySavings: Double,
    val totalMoneySaved: Double,
    val investedValue: Double,
    val inflationAdjustedValue: Double? = null
)
