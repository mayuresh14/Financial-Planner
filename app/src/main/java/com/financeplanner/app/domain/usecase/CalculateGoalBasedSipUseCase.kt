package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.GoalBasedSipInput
import com.financeplanner.app.domain.model.GoalBasedSipResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateGoalBasedSipUseCase @Inject constructor() {
    operator fun invoke(input: GoalBasedSipInput): GoalBasedSipResult {
        // If inflation is supplied, the target (given in today's terms) is
        // first inflated to what it'll actually cost at the goal date —
        // otherwise the SIP would undershoot the real future requirement.
        val effectiveTarget = input.inflationPercent?.let { inflation ->
            FinanceMath.futureCost(input.targetAmount, inflation, input.durationYears.toDouble())
        } ?: input.targetAmount

        val requiredSip = FinanceMath.requiredMonthlySip(
            targetFutureValue = effectiveTarget,
            annualReturnPercent = input.expectedReturnPercent,
            months = input.durationYears * 12
        )

        return GoalBasedSipResult(
            requiredMonthlySip = requiredSip,
            inflationAdjustedTarget = input.inflationPercent?.let { effectiveTarget }
        )
    }
}
