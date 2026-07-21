package com.financeplanner.app.domain.model

data class GratuityInput(
    val lastDrawnMonthlySalary: Double,
    val yearsOfService: Int
) {
    init {
        require(lastDrawnMonthlySalary > 0) { "Last drawn monthly salary must be positive" }
        require(yearsOfService >= 0) { "Years of service cannot be negative" }
    }
}

data class GratuityResult(
    val gratuityAmount: Double,
    val isEligible: Boolean
)
