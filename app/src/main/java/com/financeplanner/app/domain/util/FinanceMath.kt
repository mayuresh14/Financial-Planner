package com.financeplanner.app.domain.util

import kotlin.math.pow

/**
 * Pure math functions shared across calculators. No Android dependencies —
 * keeps this testable in plain JUnit and reusable if calculation logic ever
 * moves server-side (see technical design doc, Section 4: calculators have
 * no API in Phase 1/2, but the formulas staying pure keeps that option open).
 */
object FinanceMath {

    /**
     * Future value of a regular (non-step-up) SIP.
     * Standard SIP future value formula, monthly compounding:
     *   FV = P * [ ((1 + r)^n - 1) / r ] * (1 + r)
     * where r = monthly rate, n = number of months.
     */
    fun sipFutureValue(
        monthlyAmount: Double,
        annualReturnPercent: Double,
        months: Int
    ): Double {
        if (months <= 0) return 0.0
        val monthlyRate = (annualReturnPercent / 100.0) / 12.0
        if (monthlyRate == 0.0) return monthlyAmount * months
        return monthlyAmount *
            (((1 + monthlyRate).pow(months) - 1) / monthlyRate) *
            (1 + monthlyRate)
    }

    /**
     * Future value of a step-up SIP, where the monthly contribution increases
     * by [stepUpPercent] once every 12 months. Simulated month-by-month since
     * there's no closed-form formula once contributions change annually.
     */
    fun stepUpSipFutureValue(
        initialMonthlyAmount: Double,
        annualReturnPercent: Double,
        months: Int,
        stepUpPercent: Double
    ): Double {
        if (months <= 0) return 0.0
        val monthlyRate = (annualReturnPercent / 100.0) / 12.0
        var corpus = 0.0
        var currentMonthlyAmount = initialMonthlyAmount

        for (month in 1..months) {
            corpus = (corpus + currentMonthlyAmount) * (1 + monthlyRate)
            // Bump the contribution at the start of each new year (every 12 months)
            if (month % 12 == 0 && month != months) {
                currentMonthlyAmount *= (1 + stepUpPercent / 100.0)
            }
        }
        return corpus
    }

    /** Total amount invested over a step-up SIP (for "wealth gained" breakdown). */
    fun stepUpTotalInvested(
        initialMonthlyAmount: Double,
        months: Int,
        stepUpPercent: Double
    ): Double {
        var total = 0.0
        var currentMonthlyAmount = initialMonthlyAmount
        for (month in 1..months) {
            total += currentMonthlyAmount
            if (month % 12 == 0 && month != months) {
                currentMonthlyAmount *= (1 + stepUpPercent / 100.0)
            }
        }
        return total
    }

    /**
     * Future value of a one-time lumpsum investment, annual compounding:
     *   FV = P * (1 + r)^years
     */
    fun lumpsumFutureValue(
        principal: Double,
        annualReturnPercent: Double,
        years: Int
    ): Double {
        if (years <= 0) return principal
        val annualRate = annualReturnPercent / 100.0
        return principal * (1 + annualRate).pow(years)
    }

    /**
     * Inflates today's cost forward to a future year — the inverse of
     * [inflationAdjustedValue]. Used by the standalone Inflation-adjusted
     * Goal calculator and internally by Goal-based Reverse SIP.
     */
    fun futureCost(currentCost: Double, annualInflationPercent: Double, years: Double): Double {
        val inflationRate = annualInflationPercent / 100.0
        return currentCost * (1 + inflationRate).pow(years)
    }

    /**
     * Required monthly SIP to reach [targetFutureValue] — inverts
     * [sipFutureValue]: P = FV / [ (((1+r)^n - 1)/r) * (1+r) ]
     */
    fun requiredMonthlySip(
        targetFutureValue: Double,
        annualReturnPercent: Double,
        months: Int
    ): Double {
        if (months <= 0) return targetFutureValue
        val monthlyRate = (annualReturnPercent / 100.0) / 12.0
        if (monthlyRate == 0.0) return targetFutureValue / months
        val factor = (((1 + monthlyRate).pow(months) - 1) / monthlyRate) * (1 + monthlyRate)
        return targetFutureValue / factor
    }

    /**
     * Months needed for a regular SIP to reach [targetAmount]. No closed
     * form once we want an integer month count, so this steps month-by-month.
     * Capped at 100 years (1200 months) to guarantee termination for
     * unreachable targets (e.g. 0% return with a tiny SIP).
     */
    fun sipMonthsToReachTarget(
        monthlyAmount: Double,
        annualReturnPercent: Double,
        targetAmount: Double,
        maxMonths: Int = 1200
    ): Int? {
        val monthlyRate = (annualReturnPercent / 100.0) / 12.0
        var corpus = 0.0
        for (month in 1..maxMonths) {
            corpus = (corpus + monthlyAmount) * (1 + monthlyRate)
            if (corpus >= targetAmount) return month
        }
        return null // unreachable within the cap
    }

    /**
     * Future value of a yearly contribution compounding annually — the
     * annual-compounding analogue of [sipFutureValue], used by PPF/EPF-style
     * calculators where contributions and compounding both happen yearly.
     *   FV = P * [ ((1+r)^n - 1) / r ] * (1 + r)
     */
    fun annualAnnuityFutureValue(
        yearlyAmount: Double,
        annualRatePercent: Double,
        years: Int
    ): Double {
        if (years <= 0) return 0.0
        val rate = annualRatePercent / 100.0
        if (rate == 0.0) return yearlyAmount * years
        return yearlyAmount * (((1 + rate).pow(years) - 1) / rate) * (1 + rate)
    }

    /**
     * SSY maturity value. Deposits are made yearly for [depositYears] (the
     * scheme allows deposits for 15 years from account opening); the corpus
     * then continues compounding untouched for the remaining years up to
     * [maturityYears] (21 years from opening, per scheme rules).
     */
    fun ssyMaturityValue(
        yearlyDeposit: Double,
        annualRatePercent: Double,
        depositYears: Int = 15,
        maturityYears: Int = 21
    ): Double {
        val rate = annualRatePercent / 100.0
        var corpus = 0.0
        for (year in 1..maturityYears) {
            if (year <= depositYears) corpus += yearlyDeposit
            corpus *= (1 + rate)
        }
        return corpus
    }

    /**
     * Standard EMI formula: E = P * r * (1+r)^n / ((1+r)^n - 1)
     * where r is the monthly rate and n the number of months.
     */
    fun emiAmount(principal: Double, annualRatePercent: Double, months: Int): Double {
        val monthlyRate = annualRatePercent / 1200.0
        if (monthlyRate == 0.0) return principal / months
        val factor = (1 + monthlyRate).pow(months)
        return principal * monthlyRate * factor / (factor - 1)
    }

    data class LoanSimulationResult(
        val totalInterestPaid: Double,
        val monthsTaken: Int,
        val finalEmi: Double
    )

    /**
     * Simulates a loan month-by-month, optionally applying a single
     * prepayment at [prepaymentAfterMonth]. [reduceTenure] = true keeps the
     * original EMI and finishes early; false recomputes a lower EMI over
     * the original remaining tenure instead.
     */
    fun simulateLoan(
        principal: Double,
        annualRatePercent: Double,
        months: Int,
        prepaymentAmount: Double = 0.0,
        prepaymentAfterMonth: Int = Int.MAX_VALUE,
        reduceTenure: Boolean = true
    ): LoanSimulationResult {
        val monthlyRate = annualRatePercent / 1200.0
        var balance = principal
        var emi = emiAmount(principal, annualRatePercent, months)
        var totalInterest = 0.0
        var month = 0
        var remainingScheduledMonths = months

        while (balance > 0.01 && month < months * 2) {
            month++
            val interest = balance * monthlyRate
            var payment = emi
            if (payment > balance + interest) payment = balance + interest
            val principalComponent = payment - interest
            balance -= principalComponent
            totalInterest += interest
            remainingScheduledMonths--

            if (month == prepaymentAfterMonth && balance > 0) {
                balance -= prepaymentAmount
                if (balance < 0) balance = 0.0
                if (!reduceTenure && remainingScheduledMonths > 0) {
                    emi = emiAmount(balance, annualRatePercent, remainingScheduledMonths)
                }
            }
        }
        return LoanSimulationResult(totalInterest, month, emi)
    }

    data class TransferResult(val sourceRemaining: Double, val targetValue: Double)

    /**
     * STP simulation: a fixed amount transfers monthly from a source lumpsum
     * (growing at [sourceAnnualReturnPercent]) into a target fund (growing
     * at [targetAnnualReturnPercent]) for [months].
     */
    fun simulateStp(
        sourceLumpsum: Double,
        monthlyTransferAmount: Double,
        months: Int,
        sourceAnnualReturnPercent: Double,
        targetAnnualReturnPercent: Double
    ): TransferResult {
        val sourceMonthlyRate = sourceAnnualReturnPercent / 1200.0
        val targetMonthlyRate = targetAnnualReturnPercent / 1200.0
        var source = sourceLumpsum
        var target = 0.0
        repeat(months) {
            source *= (1 + sourceMonthlyRate)
            val transfer = minOf(monthlyTransferAmount, source)
            source -= transfer
            target = (target + transfer) * (1 + targetMonthlyRate)
        }
        return TransferResult(sourceRemaining = source, targetValue = target)
    }

    data class WithdrawalResult(
        val depletionMonth: Int?, // null if corpus outlasts the simulated period
        val totalWithdrawn: Double,
        val finalBalance: Double
    )

    /**
     * SWP simulation: fixed monthly withdrawal from a corpus growing at
     * [annualReturnPercent], simulated for up to [maxMonths].
     */
    fun simulateSwp(
        initialCorpus: Double,
        monthlyWithdrawal: Double,
        annualReturnPercent: Double,
        maxMonths: Int
    ): WithdrawalResult {
        val monthlyRate = annualReturnPercent / 1200.0
        var balance = initialCorpus
        var totalWithdrawn = 0.0
        for (month in 1..maxMonths) {
            balance *= (1 + monthlyRate)
            val withdrawal = minOf(monthlyWithdrawal, balance)
            balance -= withdrawal
            totalWithdrawn += withdrawal
            if (balance <= 0.01) {
                return WithdrawalResult(depletionMonth = month, totalWithdrawn = totalWithdrawn, finalBalance = 0.0)
            }
        }
        return WithdrawalResult(depletionMonth = null, totalWithdrawn = totalWithdrawn, finalBalance = balance)
    }

    /**
     * Deflates a future value back to today's purchasing power.
     *   realValue = futureValue / (1 + inflation)^years
     */
    fun inflationAdjustedValue(
        futureValue: Double,
        annualInflationPercent: Double,
        years: Double
    ): Double {
        val inflationRate = annualInflationPercent / 100.0
        return futureValue / (1 + inflationRate).pow(years)
    }
}
