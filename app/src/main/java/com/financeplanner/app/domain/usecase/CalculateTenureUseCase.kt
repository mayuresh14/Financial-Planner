package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.TenureInput
import com.financeplanner.app.domain.model.TenureResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateTenureUseCase @Inject constructor() {
    operator fun invoke(input: TenureInput): TenureResult {
        val months = FinanceMath.sipMonthsToReachTarget(
            monthlyAmount = input.monthlyAmount,
            annualReturnPercent = input.expectedReturnPercent,
            targetAmount = input.targetAmount
        )
        return TenureResult(
            monthsRequired = months,
            yearsRequired = months?.let { it / 12.0 }
        )
    }
}
