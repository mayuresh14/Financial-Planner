package com.financeplanner.app.domain.model

data class TenureInput(
    val monthlyAmount: Double,
    val expectedReturnPercent: Double,
    val targetAmount: Double
) {
    init {
        require(monthlyAmount > 0) { "Monthly amount must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        require(targetAmount > 0) { "Target amount must be positive" }
    }
}

data class TenureResult(
    val monthsRequired: Int?, // null if unreachable within the calculation cap
    val yearsRequired: Double?
)
