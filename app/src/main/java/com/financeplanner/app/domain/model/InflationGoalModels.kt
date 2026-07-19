package com.financeplanner.app.domain.model

data class InflationGoalInput(
    val currentCost: Double,
    val inflationPercent: Double,
    val years: Int
) {
    init {
        require(currentCost > 0) { "Current cost must be positive" }
        require(inflationPercent >= 0) { "Inflation percent cannot be negative" }
        require(years > 0) { "Years must be at least 1" }
    }
}

data class InflationGoalResult(
    val futureCost: Double,
    val increaseAmount: Double
)
