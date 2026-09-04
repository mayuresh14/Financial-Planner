package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.AlcoholInput
import com.financeplanner.app.domain.model.AlcoholResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateAlcoholUseCase @Inject constructor() {
    operator fun invoke(input: AlcoholInput): AlcoholResult {
        val monthlySpend = input.drinksPerWeek * (52.0 / 12.0) * input.costPerDrink
        val months = input.years * 12
        val totalMoneySpent = monthlySpend * months
        val investedValue = FinanceMath.sipFutureValue(
            monthlyAmount = monthlySpend,
            annualReturnPercent = input.expectedReturnPercent,
            months = months
        )
        return AlcoholResult(
            monthlySpend = monthlySpend,
            totalMoneySpent = totalMoneySpent,
            investedValue = investedValue
        )
    }
}
