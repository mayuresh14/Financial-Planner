package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.EpfInput
import com.financeplanner.app.domain.model.EpfResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateEpfUseCase @Inject constructor() {
    operator fun invoke(input: EpfInput): EpfResult {
        val monthlyContribution = input.monthlyContribution

        val maturityValue = FinanceMath.sipFutureValue(
            monthlyAmount = monthlyContribution,
            annualReturnPercent = input.expectedReturnPercent,
            months = input.durationYears * 12
        ) + FinanceMath.lumpsumFutureValue(
            principal = input.existingBalance,
            annualReturnPercent = input.expectedReturnPercent,
            years = input.durationYears
        )
        val totalContributed = monthlyContribution * input.durationYears * 12 + input.existingBalance
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
