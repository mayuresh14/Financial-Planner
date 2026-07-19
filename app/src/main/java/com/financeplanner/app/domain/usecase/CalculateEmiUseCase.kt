package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.EmiInput
import com.financeplanner.app.domain.model.EmiResult
import com.financeplanner.app.domain.model.PrepaymentStrategy
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateEmiUseCase @Inject constructor() {
    operator fun invoke(input: EmiInput): EmiResult {
        val baseline = FinanceMath.simulateLoan(
            principal = input.loanAmount,
            annualRatePercent = input.interestRatePercent,
            months = input.tenureMonths
        )

        if (input.prepaymentAmount == null || input.prepaymentAfterMonth == null) {
            return EmiResult(
                emi = baseline.finalEmi,
                totalInterestWithoutPrepayment = baseline.totalInterestPaid
            )
        }

        val withPrepayment = FinanceMath.simulateLoan(
            principal = input.loanAmount,
            annualRatePercent = input.interestRatePercent,
            months = input.tenureMonths,
            prepaymentAmount = input.prepaymentAmount,
            prepaymentAfterMonth = input.prepaymentAfterMonth,
            reduceTenure = input.prepaymentStrategy == PrepaymentStrategy.REDUCE_TENURE
        )

        return EmiResult(
            emi = baseline.finalEmi,
            totalInterestWithoutPrepayment = baseline.totalInterestPaid,
            totalInterestWithPrepayment = withPrepayment.totalInterestPaid,
            monthsSaved = if (input.prepaymentStrategy == PrepaymentStrategy.REDUCE_TENURE) {
                baseline.monthsTaken - withPrepayment.monthsTaken
            } else null,
            interestSaved = baseline.totalInterestPaid - withPrepayment.totalInterestPaid,
            newEmiAfterPrepayment = if (input.prepaymentStrategy == PrepaymentStrategy.REDUCE_EMI) {
                withPrepayment.finalEmi
            } else null
        )
    }
}
