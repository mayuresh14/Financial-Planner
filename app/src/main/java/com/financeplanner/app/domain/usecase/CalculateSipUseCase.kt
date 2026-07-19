package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

/**
 * Calculates SIP maturity value, handling both regular and step-up SIPs,
 * with an optional inflation-adjusted view. This is the template pattern
 * every other calculator's UseCase follows: plain constructor injection,
 * a single [invoke] operator, no Android framework dependencies so it's
 * trivially unit-testable and portable if calculation ever moves server-side.
 */
class CalculateSipUseCase @Inject constructor() {

    operator fun invoke(input: SipInput): SipResult {
        val months = input.durationYears * 12

        val maturityValue: Double
        val totalInvested: Double

        if (input.stepUpPercent != null && input.stepUpPercent > 0) {
            maturityValue = FinanceMath.stepUpSipFutureValue(
                initialMonthlyAmount = input.monthlyAmount,
                annualReturnPercent = input.expectedReturnPercent,
                months = months,
                stepUpPercent = input.stepUpPercent
            )
            totalInvested = FinanceMath.stepUpTotalInvested(
                initialMonthlyAmount = input.monthlyAmount,
                months = months,
                stepUpPercent = input.stepUpPercent
            )
        } else {
            maturityValue = FinanceMath.sipFutureValue(
                monthlyAmount = input.monthlyAmount,
                annualReturnPercent = input.expectedReturnPercent,
                months = months
            )
            totalInvested = input.monthlyAmount * months
        }

        val inflationAdjustedValue = input.inflationPercent?.let { inflation ->
            FinanceMath.inflationAdjustedValue(
                futureValue = maturityValue,
                annualInflationPercent = inflation,
                years = input.durationYears.toDouble()
            )
        }

        return SipResult(
            maturityValue = maturityValue,
            totalInvested = totalInvested,
            wealthGained = maturityValue - totalInvested,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }
}
