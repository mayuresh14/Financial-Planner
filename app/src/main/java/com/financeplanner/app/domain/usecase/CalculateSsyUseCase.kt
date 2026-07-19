package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SsyInput
import com.financeplanner.app.domain.model.SsyResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateSsyUseCase @Inject constructor() {
    operator fun invoke(input: SsyInput): SsyResult {
        val maturityValue = FinanceMath.ssyMaturityValue(
            yearlyDeposit = input.yearlyDeposit,
            annualRatePercent = input.interestRatePercent
        )
        val depositYears = minOf(15, 21)
        val totalDeposited = input.yearlyDeposit * depositYears
        return SsyResult(
            maturityValue = maturityValue,
            totalDeposited = totalDeposited,
            interestEarned = maturityValue - totalDeposited,
            girlAgeAtMaturity = input.girlAgeAtOpening + 21
        )
    }
}
