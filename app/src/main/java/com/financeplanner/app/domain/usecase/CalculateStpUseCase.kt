package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.StpInput
import com.financeplanner.app.domain.model.StpResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateStpUseCase @Inject constructor() {
    operator fun invoke(input: StpInput): StpResult {
        val result = FinanceMath.simulateStp(
            sourceLumpsum = input.sourceLumpsum,
            monthlyTransferAmount = input.monthlyTransferAmount,
            months = input.durationMonths,
            sourceAnnualReturnPercent = input.sourceReturnPercent,
            targetAnnualReturnPercent = input.targetReturnPercent
        )
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = result.targetValue,
                annualInflationPercent = inflation,
                years = input.durationMonths / 12.0
            )
        }
        return StpResult(
            sourceRemaining = result.sourceRemaining,
            targetValue = result.targetValue,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
