package com.financeplanner.app.domain.model

data class StpInput(
    val sourceLumpsum: Double,
    val monthlyTransferAmount: Double,
    val durationMonths: Int,
    val sourceReturnPercent: Double,
    val targetReturnPercent: Double
) {
    init {
        require(sourceLumpsum > 0) { "Source lumpsum must be positive" }
        require(monthlyTransferAmount > 0) { "Monthly transfer amount must be positive" }
        require(durationMonths > 0) { "Duration must be at least 1 month" }
        require(sourceReturnPercent >= 0) { "Source return cannot be negative" }
        require(targetReturnPercent >= 0) { "Target return cannot be negative" }
    }
}

data class StpResult(
    val sourceRemaining: Double,
    val targetValue: Double
)
