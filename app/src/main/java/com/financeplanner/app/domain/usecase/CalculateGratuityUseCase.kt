package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.GratuityInput
import com.financeplanner.app.domain.model.GratuityResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

class CalculateGratuityUseCase @Inject constructor() {
    operator fun invoke(input: GratuityInput): GratuityResult {
        val gratuityAmount = FinanceMath.gratuityAmount(
            lastDrawnMonthlySalary = input.lastDrawnMonthlySalary,
            yearsOfService = input.yearsOfService
        )
        return GratuityResult(
            gratuityAmount = gratuityAmount,
            isEligible = input.yearsOfService >= 5
        )
    }
}
