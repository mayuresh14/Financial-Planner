package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.EpfInput
import com.financeplanner.app.domain.model.EpfResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateEpfUseCase @Inject constructor() {
    operator fun invoke(input: EpfInput): EpfResult {
        val monthlyContribution = input.basicMonthlySalary *
            (input.employeeContributionPercent + input.employerContributionPercent) / 100.0

        val maturityValue = FinanceMath.sipFutureValue(
            monthlyAmount = monthlyContribution,
            annualReturnPercent = input.expectedReturnPercent,
            months = input.durationYears * 12
        )
        val totalContributed = monthlyContribution * input.durationYears * 12
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = maturityValue,
                annualInflationPercent = inflation,
                years = input.durationYears.toDouble()
            )
        }
        return EpfResult(
            maturityValue = maturityValue,
            totalContributed = totalContributed,
            interestEarned = maturityValue - totalContributed,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
