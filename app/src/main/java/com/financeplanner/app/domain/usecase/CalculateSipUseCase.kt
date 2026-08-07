package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipResult
import com.financeplanner.app.domain.util.FinanceMath
import javax.inject.Inject

/**
 * Calculates SIP maturity value, handling regular and step-up SIPs (either
 * percentage or fixed-amount step-up), daily/weekly/monthly contribution
 * frequency, an optional expense-ratio drag on the return, and an optional
 * inflation-adjusted view. This is the template pattern every other
 * calculator's UseCase follows: plain constructor injection, a single
 * [invoke] operator, no Android framework dependencies so it's trivially
 * unit-testable and portable if calculation ever moves server-side.
 */
class CalculateSipUseCase @Inject constructor() {

    operator fun invoke(input: SipInput): SipResult {
        val periodsPerYear = input.frequency.periodsPerYear
        val totalPeriods = input.durationYears * periodsPerYear

        // Expense ratio is modeled as a straight drag on the gross expected
        // return — the standard simplified way fund costs reduce net growth.
        val expenseRatio = input.expenseRatioPercent ?: 0.0
        val netReturnPercent = (input.expectedReturnPercent - expenseRatio).coerceAtLeast(0.0)

        val stepUpPercent = input.stepUpPercent ?: 0.0
        val stepUpFixedAmount = input.stepUpFixedAmount ?: 0.0
        val hasStepUp = stepUpPercent > 0 || stepUpFixedAmount > 0

        val initialLumpsum = input.initialLumpsum ?: 0.0
        val lumpsumMaturityValue = if (initialLumpsum > 0) {
            FinanceMath.lumpsumFutureValue(initialLumpsum, netReturnPercent, input.durationYears)
        } else {
            0.0
        }

        val maturityValue = growthFor(input, netReturnPercent, totalPeriods, periodsPerYear, hasStepUp, stepUpPercent, stepUpFixedAmount) +
            lumpsumMaturityValue
        val totalInvested = (if (hasStepUp) {
            FinanceMath.stepUpTotalInvested(
                initialAmount = input.contributionAmount,
                totalPeriods = totalPeriods,
                stepUpPercent = stepUpPercent,
                stepUpFixedAmount = stepUpFixedAmount,
                periodsPerYear = periodsPerYear
            )
        } else {
            input.contributionAmount * totalPeriods
        }) + initialLumpsum

        // Expense amount: how much smaller the corpus is versus what it would
        // have been at the gross (pre-expense) return — i.e. the fees' cost.
        val expenseAmount = if (expenseRatio > 0) {
            val grossLumpsumMaturityValue = if (initialLumpsum > 0) {
                FinanceMath.lumpsumFutureValue(initialLumpsum, input.expectedReturnPercent, input.durationYears)
            } else {
                0.0
            }
            val grossMaturityValue = growthFor(
                input, input.expectedReturnPercent, totalPeriods, periodsPerYear, hasStepUp, stepUpPercent, stepUpFixedAmount
            ) + grossLumpsumMaturityValue
            (grossMaturityValue - maturityValue).coerceAtLeast(0.0)
        } else {
            null
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
            expenseAmount = expenseAmount,
            inflationAdjustedValue = inflationAdjustedValue
        )
    }

    private fun growthFor(
        input: SipInput,
        annualReturnPercent: Double,
        totalPeriods: Int,
        periodsPerYear: Int,
        hasStepUp: Boolean,
        stepUpPercent: Double,
        stepUpFixedAmount: Double
    ): Double = if (hasStepUp) {
        FinanceMath.stepUpSipFutureValue(
            initialAmount = input.contributionAmount,
            annualReturnPercent = annualReturnPercent,
            totalPeriods = totalPeriods,
            stepUpPercent = stepUpPercent,
            stepUpFixedAmount = stepUpFixedAmount,
            periodsPerYear = periodsPerYear
        )
    } else {
        FinanceMath.sipFutureValue(
            monthlyAmount = input.contributionAmount,
            annualReturnPercent = annualReturnPercent,
            months = totalPeriods,
            periodsPerYear = periodsPerYear
        )
    }
}
