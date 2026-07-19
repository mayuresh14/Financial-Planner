package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.FireInput
import com.financeplanner.app.domain.model.FireResult
import com.financeplanner.app.domain.model.FireVariant
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject
import kotlin.math.pow

/**
 * Expense multiples are standard FIRE-community rules of thumb, not
 * regulatory figures — Traditional uses the classic 4% safe withdrawal
 * rate (25x expenses); Lean and Fat scale that multiple down/up for a
 * frugal vs. comfortable retirement lifestyle. Coast uses the Traditional
 * multiple as its target, since "coasting" means existing savings alone
 * grow to a standard retirement corpus without further contributions.
 */
class CalculateFireUseCase @Inject constructor() {

    operator fun invoke(input: FireInput): FireResult {
        val yearsToRetirement = input.retirementAge - input.currentAge
        val expensesAtRetirement = FinanceMath.futureCost(
            currentCost = input.currentAnnualExpenses,
            annualInflationPercent = input.inflationPercent,
            years = yearsToRetirement.toDouble()
        )

        val multiple = when (input.variant) {
            FireVariant.TRADITIONAL, FireVariant.COAST -> 25.0
            FireVariant.LEAN -> 20.0
            FireVariant.FAT -> 33.0
        }
        val requiredCorpus = expensesAtRetirement * multiple

        val existingCorpusFutureValue = input.existingCorpus *
            (1 + input.preRetirementReturnPercent / 100.0).pow(yearsToRetirement)

        val shortfall = (requiredCorpus - existingCorpusFutureValue).coerceAtLeast(0.0)
        val requiredMonthlySip = if (shortfall > 0) {
            FinanceMath.requiredMonthlySip(
                targetFutureValue = shortfall,
                annualReturnPercent = input.preRetirementReturnPercent,
                months = yearsToRetirement * 12
            )
        } else 0.0

        return FireResult(
            requiredCorpus = requiredCorpus,
            requiredMonthlySip = requiredMonthlySip,
            isCoastFireAchieved = if (input.variant == FireVariant.COAST) {
                existingCorpusFutureValue >= requiredCorpus
            } else null
        )
    }
}
