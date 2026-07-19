package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SwpInput
import com.financeplanner.app.domain.model.SwpResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateSwpUseCase @Inject constructor() {
    operator fun invoke(input: SwpInput): SwpResult {
        val result = FinanceMath.simulateSwp(
            initialCorpus = input.initialCorpus,
            monthlyWithdrawal = input.monthlyWithdrawal,
            annualReturnPercent = input.expectedReturnPercent,
            maxMonths = input.simulateYears * 12
        )
        return SwpResult(
            depletionMonth = result.depletionMonth,
            totalWithdrawn = result.totalWithdrawn,
            finalBalance = result.finalBalance
        )
    }
}
