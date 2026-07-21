package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.EatingOutInput
import com.financeplanner.app.domain.model.EatingOutResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateEatingOutUseCase @Inject constructor() {
    operator fun invoke(input: EatingOutInput): EatingOutResult {
        val monthlySavings = input.eatingOutMonthlySpend - input.homeCookingMonthlySpend
        val months = input.years * 12
        val totalMoneySaved = monthlySavings * months
        val investedValue = FinanceMath.sipFutureValue(
            monthlyAmount = monthlySavings,
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
        return EatingOutResult(
            monthlySavings = monthlySavings,
            totalMoneySaved = totalMoneySaved,
            investedValue = investedValue,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
