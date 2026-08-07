package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.FireAgeInput
import com.financeplanner.app.domain.model.FireAgeResult
import com.financeplanner.app.domain.model.FireVariant
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

/**
 * Reverse of [CalculateFireUseCase]: instead of solving for the SIP needed
 * to retire at a chosen age, this solves for the age reachable with a fixed
 * monthly SIP (plus any existing corpus), by simulating both the growing
 * corpus and the inflating FIRE target month-by-month until they cross.
 */
class CalculateFireAgeUseCase @Inject constructor() {

    operator fun invoke(input: FireAgeInput): FireAgeResult {
        val multiple = when (input.variant) {
            FireVariant.TRADITIONAL, FireVariant.COAST -> 25.0
            FireVariant.LEAN -> 20.0
            FireVariant.FAT -> 33.0
        }

        val months = FinanceMath.monthsToReachGrowingTarget(
            existingCorpus = input.existingCorpus,
            monthlyContribution = input.monthlySip,
            preRetirementReturnPercent = input.preRetirementReturnPercent,
            currentAnnualExpenses = input.currentAnnualExpenses,
            inflationPercent = input.inflationPercent,
            expenseMultiple = multiple
        )

        if (months == null) {
            return FireAgeResult(fireAge = null, yearsToFire = null, requiredCorpusAtFire = null)
        }

        val yearsToFire = months / 12.0
        val requiredCorpusAtFire = FinanceMath.futureCost(
            currentCost = input.currentAnnualExpenses,
            annualInflationPercent = input.inflationPercent,
            years = yearsToFire
        ) * multiple

        return FireAgeResult(
            fireAge = input.currentAge + Math.ceil(yearsToFire).toInt(),
            yearsToFire = yearsToFire,
            requiredCorpusAtFire = requiredCorpusAtFire
        )
    }
}
