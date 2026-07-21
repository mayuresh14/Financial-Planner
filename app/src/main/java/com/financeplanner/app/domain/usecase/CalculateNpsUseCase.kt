package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.NpsInput
import com.financeplanner.app.domain.model.NpsResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

private const val NPS_RETIREMENT_AGE = 60
private const val ASSUMED_ANNUITY_RATE_PERCENT = 6.0 // typical current annuity payout rate; user's actual rate varies by provider

class CalculateNpsUseCase @Inject constructor() {
    operator fun invoke(input: NpsInput): NpsResult {
        val months = (NPS_RETIREMENT_AGE - input.currentAge) * 12
        val corpus = FinanceMath.sipFutureValue(
            monthlyAmount = input.monthlyContribution,
            annualReturnPercent = input.expectedReturnPercent,
            months = months
        )
        val annuityCorpus = corpus * (input.annuityPercent / 100.0)
        val lumpsum = corpus - annuityCorpus
        val monthlyPension = annuityCorpus * (ASSUMED_ANNUITY_RATE_PERCENT / 100.0) / 12.0
        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = corpus,
                annualInflationPercent = inflation,
                years = (NPS_RETIREMENT_AGE - input.currentAge).toDouble()
            )
        }

        return NpsResult(
            corpusAtSixty = corpus,
            lumpsumWithdrawal = lumpsum,
            annuityCorpus = annuityCorpus,
            estimatedMonthlyPension = monthlyPension,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
