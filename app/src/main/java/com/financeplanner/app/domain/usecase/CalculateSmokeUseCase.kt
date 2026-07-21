package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SmokeInput
import com.financeplanner.app.domain.model.SmokeResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateSmokeUseCase @Inject constructor() {
    operator fun invoke(input: SmokeInput): SmokeResult {
        val monthlySpend = input.cigarettesPerDay * 30.0 * input.costPerCigarette
        val months = input.years * 12
        val totalMoneySpent = monthlySpend * months
        val investedValue = FinanceMath.sipFutureValue(
            monthlyAmount = monthlySpend,
            annualReturnPercent = input.expectedReturnPercent,
            months = months
        )
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = investedValue,
                annualInflationPercent = inflation,
                years = input.years.toDouble()
            )
        }
        return SmokeResult(
            monthlySpend = monthlySpend,
            totalMoneySpent = totalMoneySpent,
            investedValue = investedValue,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
