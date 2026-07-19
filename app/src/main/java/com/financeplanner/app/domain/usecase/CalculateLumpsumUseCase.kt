package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.LumpsumInput
import com.financeplanner.app.domain.model.LumpsumResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateLumpsumUseCase @Inject constructor() {

    operator fun invoke(input: LumpsumInput): LumpsumResult {
        val maturityValue = FinanceMath.lumpsumFutureValue(
            principal = input.principal,
            annualReturnPercent = input.expectedReturnPercent,
            years = input.durationYears
        )

        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = maturityValue,
                annualInflationPercent = inflation,
                years = input.durationYears.toDouble()
            )
        }

        return LumpsumResult(
            maturityValue = maturityValue,
            principal = input.principal,
            wealthGained = maturityValue - input.principal,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
