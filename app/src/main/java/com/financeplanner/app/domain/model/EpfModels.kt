package com.financeplanner.app.domain.model

data class EpfInput(
    val basicMonthlySalary: Double,
    val employeeContributionPercent: Double = 12.0,
    val employerContributionPercent: Double = 12.0,
    val durationYears: Int,
    val expectedReturnPercent: Double = 8.25 // current EPF rate as of last known notification; user-editable
) {
    init {
        require(basicMonthlySalary > 0) { "Basic salary must be positive" }
        require(employeeContributionPercent > 0) { "Employee contribution percent must be positive" }
        require(employerContributionPercent >= 0) { "Employer contribution percent cannot be negative" }
        require(durationYears > 0) { "Duration must be at least 1 year" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
    }
}

data class EpfResult(
    val maturityValue: Double,
    val totalContributed: Double,
    val interestEarned: Double
)
