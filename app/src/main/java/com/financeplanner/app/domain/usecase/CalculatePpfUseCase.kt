package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.PpfInput
import com.financeplanner.app.domain.model.PpfResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculatePpfUseCase @Inject constructor() {
    operator fun invoke(input: PpfInput): PpfResult {
        val maturityValue = FinanceMath.annualAnnuityFutureValue(
            yearlyAmount = input.yearlyContribution,
            annualRatePercent = input.interestRatePercent,
            years = input.durationYears
        ) + FinanceMath.lumpsumFutureValue(
            principal = input.existingBalance,
            annualReturnPercent = input.interestRatePercent,
            years = input.durationYears
        )
        val totalContributed = input.yearlyContribution * input.durationYears + input.existingBalance
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = maturityValue,
                annualInflationPercent = inflation,
                years = input.durationYears.toDouble()
            )
        }
        return PpfResult(
            maturityValue = maturityValue,
            totalContributed = totalContributed,
            interestEarned = maturityValue - totalContributed,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
