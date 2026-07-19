package com.financeplanner.app.domain.model

data class SwpInput(
    val initialCorpus: Double,
    val monthlyWithdrawal: Double,
    val expectedReturnPercent: Double,
    val simulateYears: Int = 30
) {
    init {
        require(initialCorpus > 0) { "Initial corpus must be positive" }
        require(monthlyWithdrawal > 0) { "Monthly withdrawal must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        require(simulateYears > 0) { "Simulation period must be at least 1 year" }
    }
}

data class SwpResult(
    val depletionMonth: Int?,
    val totalWithdrawn: Double,
    val finalBalance: Double
)
