package com.financeplanner.app.domain.model

enum class PrepaymentStrategy { REDUCE_TENURE, REDUCE_EMI }

data class EmiInput(
    val loanAmount: Double,
    val interestRatePercent: Double,
    val tenureMonths: Int,
    val prepaymentAmount: Double? = null,
    val prepaymentAfterMonth: Int? = null,
    val prepaymentStrategy: PrepaymentStrategy = PrepaymentStrategy.REDUCE_TENURE
) {
    init {
        require(loanAmount > 0) { "Loan amount must be positive" }
        require(interestRatePercent >= 0) { "Interest rate cannot be negative" }
        require(tenureMonths > 0) { "Tenure must be at least 1 month" }
        prepaymentAmount?.let { require(it > 0) { "Prepayment amount must be positive" } }
        prepaymentAfterMonth?.let { require(it in 1 until tenureMonths) { "Prepayment month must fall within the loan tenure" } }
    }
}

data class EmiResult(
    val emi: Double,
    val totalInterestWithoutPrepayment: Double,
    val totalInterestWithPrepayment: Double? = null,
    val monthsSaved: Int? = null,
    val interestSaved: Double? = null,
    val newEmiAfterPrepayment: Double? = null
)
