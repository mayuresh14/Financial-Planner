package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.RdInput
import com.financeplanner.app.domain.model.RdResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateRdUseCase @Inject constructor() {
    operator fun invoke(input: RdInput): RdResult {
        val maturityValue = FinanceMath.rdMaturityValue(
            monthlyDeposit = input.monthlyDeposit,
            annualRatePercent = input.annualRatePercent,
            tenureMonths = input.tenureMonths
        )
        val totalDeposited = input.monthlyDeposit * input.tenureMonths
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = maturityValue,
                annualInflationPercent = inflation,
                years = input.tenureMonths / 12.0
            )
        }
        return RdResult(
            maturityValue = maturityValue,
            totalDeposited = totalDeposited,
            interestEarned = maturityValue - totalDeposited,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
