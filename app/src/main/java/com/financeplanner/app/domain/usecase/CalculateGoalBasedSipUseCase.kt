package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.GoalBasedSipInput
import com.financeplanner.app.domain.model.GoalBasedSipResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateGoalBasedSipUseCase @Inject constructor() {
    operator fun invoke(input: GoalBasedSipInput): GoalBasedSipResult {
        // The primary result targets the amount exactly as entered, matching
        // every other calculator's pattern where inflation only annotates the
        // display rather than silently changing the headline number.
        val requiredSip = FinanceMath.requiredMonthlySip(
            targetFutureValue = input.targetAmount,
            annualReturnPercent = input.expectedReturnPercent,
            months = input.durationYears * 12
        )

        val inflatedTarget = input.inflationPercent?.let { inflation ->
            FinanceMath.futureCost(input.targetAmount, inflation, input.durationYears.toDouble())
        }
        val sipForInflatedTarget = inflatedTarget?.let {
            FinanceMath.requiredMonthlySip(
                targetFutureValue = it,
                annualReturnPercent = input.expectedReturnPercent,
                months = input.durationYears * 12
            )
        }

        return GoalBasedSipResult(
            requiredMonthlySip = requiredSip,
            inflationAdjustedTarget = inflatedTarget,
            sipForInflationAdjustedTarget = sipForInflatedTarget
        )
    }
}
