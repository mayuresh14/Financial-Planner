package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.InflationGoalInput
import com.financeplanner.app.domain.model.InflationGoalResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateInflationGoalUseCase @Inject constructor() {
    operator fun invoke(input: InflationGoalInput): InflationGoalResult {
        val futureCost = FinanceMath.futureCost(
            currentCost = input.currentCost,
            annualInflationPercent = input.inflationPercent,
            years = input.years.toDouble()
        )
        return InflationGoalResult(
            futureCost = futureCost,
            increaseAmount = futureCost - input.currentCost
        )
    }
}
