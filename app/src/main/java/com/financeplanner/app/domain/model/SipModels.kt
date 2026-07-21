package com.financeplanner.app.domain.model

/** How often the SIP contribution is invested — changes both what the entered amount means and the compounding frequency. */
enum class SipFrequency(val periodsPerYear: Int) {
    DAILY(365),
    WEEKLY(52),
    MONTHLY(12)
}

/**
 * Input for the SIP calculator. [stepUpPercent], [stepUpFixedAmount],
 * [expenseRatioPercent], and [inflationPercent] are all optional advanced
 * settings. [stepUpPercent] and [stepUpFixedAmount] are mutually exclusive —
 * pick one way to step up the contribution each year, not both.
 */
data class SipInput(
    val contributionAmount: Double,
    val expectedReturnPercent: Double,
    val durationYears: Int,
    val frequency: SipFrequency = SipFrequency.MONTHLY,
    val stepUpPercent: Double? = null,
    val stepUpFixedAmount: Double? = null,
    val expenseRatioPercent: Double? = null,
    val inflationPercent: Double? = null
) {
    init {
        require(contributionAmount > 0) { "Contribution amount must be positive" }
        require(expectedReturnPercent >= 0) { "Expected return cannot be negative" }
        require(durationYears > 0) { "Duration must be at least 1 year" }
        stepUpPercent?.let { require(it >= 0) { "Step-up percent cannot be negative" } }
        stepUpFixedAmount?.let { require(it >= 0) { "Step-up fixed amount cannot be negative" } }
        require(!((stepUpPercent ?: 0.0) > 0 && (stepUpFixedAmount ?: 0.0) > 0)) {
            "Choose either a percentage step-up or a fixed step-up amount, not both"
        }
        expenseRatioPercent?.let { require(it >= 0) { "Expense ratio cannot be negative" } }
        inflationPercent?.let { require(it >= 0) { "Inflation percent cannot be negative" } }
    }
}

/**
 * Result of a SIP calculation. [maturityValue] already reflects the net
 * (post-expense-ratio) return — [expenseAmount] is shown separately just to
 * make that cost visible, not subtracted again. Both [expenseAmount] and
 * [inflationAdjustedValue] are null when the user didn't supply that input.
 */
data class SipResult(
    val maturityValue: Double,
    val totalInvested: Double,
    val wealthGained: Double,
    val expenseAmount: Double? = null,
    val inflationAdjustedValue: Double? = null
)
