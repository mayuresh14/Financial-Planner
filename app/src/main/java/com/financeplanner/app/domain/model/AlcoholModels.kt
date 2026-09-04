package com.financeplanner.app.domain.model

data class AlcoholInput(
    val drinksPerWeek: Int,
    val costPerDrink: Double,
    val years: Int,
    val expectedReturnPercent: Double
) {
    init {
        require(drinksPerWeek > 0) { "Drinks per week must be positive" }
        require(costPerDrink > 0) { "Cost per drink must be positive" }
        require(years > 0) { "Years must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
    }
}

data class AlcoholResult(
    val monthlySpend: Double,
    val totalMoneySpent: Double,
    val investedValue: Double
)
