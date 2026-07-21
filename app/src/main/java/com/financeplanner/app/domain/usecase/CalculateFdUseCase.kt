package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.FdInput
import com.financeplanner.app.domain.model.FdResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateFdUseCase @Inject constructor() {
    operator fun invoke(input: FdInput): FdResult {
        val maturityValue = FinanceMath.fdMaturityValue(
            principal = input.principal,
            annualRatePercent = input.annualRatePercent,
            tenureMonths = input.tenureMonths
        )
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = maturityValue,
                annualInflationPercent = inflation,
                years = input.tenureMonths / 12.0
            )
        }
        return FdResult(
            maturityValue = maturityValue,
            totalInterestEarned = maturityValue - input.principal,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
