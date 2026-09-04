package com.financeplanner.app.domain.model

data class EpfInput(
    /** Set when contribution is computed from salary × percentages — mutually exclusive
     * with [flatMonthlyContribution] (exactly one of the two must be provided). */
    val basicMonthlySalary: Double? = null,
    val employeeContributionPercent: Double = 12.0,
    val employerContributionPercent: Double = 12.0,
    /** Set when the user knows their combined monthly EPF contribution directly and would
     * rather type that than back-derive it from salary/percentages. */
    val flatMonthlyContribution: Double? = null,
    val durationYears: Int,
    val expectedReturnPercent: Double = 8.25, // current EPF rate as of last known notification; user-editable
    val inflationPercent: Double? = null,
    /** Balance already in the account before this projection starts — for someone tracking
     * an account they opened before using this app, not one starting from zero. */
    val existingBalance: Double = 0.0
) {
    init {
        require(basicMonthlySalary != null || flatMonthlyContribution != null) {
            "Either a basic salary or a flat monthly contribution must be provided"
        }
        basicMonthlySalary?.let {
            require(it > 0) { "Basic salary must be positive" }
            require(employeeContributionPercent > 0) { "Employee contribution percent must be positive" }
            require(employerContributionPercent >= 0) { "Employer contribution percent cannot be negative" }
        }
        flatMonthlyContribution?.let { require(it > 0) { "Monthly contribution must be positive" } }
        require(durationYears > 0) { "Duration must be at least 1 year" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
        require(existingBalance >= 0) { "Existing balance cannot be negative" }
    }

    /** The figure actually used for projection, regardless of which input mode was used. */
    val monthlyContribution: Double
        get() = flatMonthlyContribution
            ?: (basicMonthlySalary!! * (employeeContributionPercent + employerContributionPercent) / 100.0)
}

data class EpfResult(
    val maturityValue: Double,
    val totalContributed: Double,
    val interestEarned: Double,
    val inflationAdjustedValue: Double? = null
)
