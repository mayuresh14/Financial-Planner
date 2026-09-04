package com.financeplanner.app.ui.common

import java.text.NumberFormat
import java.util.Locale

private val currencyFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
}

private fun Map<String, String>.num(key: String): Double? = this[key]?.toDoubleOrNull()

private fun money(value: Double?): String? = value?.let { currencyFormat.format(it) }

/**
 * Short, plain-English recap of what a saved item's inputs mean — the same idea as each
 * calculator's on-screen result narrative, but generic and reconstructed purely from the
 * persisted field map, since we don't re-run every calculator's use case here. Returns null
 * when there isn't enough saved data to say anything useful (e.g. very old entries).
 */
fun buildSavedItemNarrative(typeName: String, fields: Map<String, String>, lastComputedValue: Double?): String? {
    val maturity = money(lastComputedValue)
    return when (typeName) {
        "SIP" -> {
            val monthly = money(fields.num("monthlyAmount")) ?: return null
            val years = fields["durationYears"] ?: return null
            val stepUp = fields.num("stepUpPercent")?.takeIf { it > 0 }?.let { " with a $it% yearly step-up" }
                ?: fields.num("stepUpFixedAmount")?.takeIf { it > 0 }?.let { " with a ${money(it)} yearly step-up" }
                ?: ""
            "Investing $monthly every month for $years years$stepUp" +
                (maturity?.let { ", this is projected to grow to $it." } ?: ".")
        }
        "LUMPSUM" -> {
            val principal = money(fields.num("principal")) ?: return null
            val years = fields["durationYears"] ?: return null
            "A one-time investment of $principal, left to grow for $years years" +
                (maturity?.let { ", is projected to become $it." } ?: ".")
        }
        "GOAL_BASED_SIP" -> {
            val target = money(fields.num("targetAmount")) ?: return null
            val years = fields["durationYears"] ?: return null
            "To reach a goal of $target in $years years, this plan works out the monthly SIP needed" +
                (lastComputedValue?.let { " — about ${currencyFormat.format(it)} a month." } ?: ".")
        }
        "FD" -> {
            val principal = money(fields.num("principal")) ?: return null
            val tenure = fields["tenureMonths"] ?: return null
            "A fixed deposit of $principal for $tenure months" +
                (maturity?.let { " matures to $it." } ?: ".")
        }
        "RD" -> {
            val monthly = money(fields.num("monthlyDeposit")) ?: return null
            val tenure = fields["tenureMonths"] ?: return null
            "Depositing $monthly every month for $tenure months" +
                (maturity?.let { " matures to $it." } ?: ".")
        }
        "PPF" -> {
            val isMonthly = fields["contributionFrequency"] == "MONTHLY"
            val contribution = if (isMonthly) money(fields.num("monthlyContribution")) else money(fields.num("yearlyContribution"))
            val contribution2 = contribution ?: return null
            val cadence = if (isMonthly) "month" else "year"
            val years = fields["durationYears"] ?: return null
            "Contributing $contribution2 every $cadence for $years years" +
                (maturity?.let { ", this PPF is projected to reach $it at maturity." } ?: ".")
        }
        "EPF" -> {
            val isFlat = fields["contributionMode"] == "FLAT_AMOUNT"
            val monthly = if (isFlat) {
                money(fields.num("flatMonthlyContribution"))
            } else {
                val salary = fields.num("basicMonthlySalary")
                val pct = (fields.num("employeeContributionPercent") ?: 0.0) + (fields.num("employerContributionPercent") ?: 0.0)
                salary?.let { money(it * pct / 100.0) }
            }
            val monthly2 = monthly ?: return null
            val years = fields["durationYears"] ?: return null
            "A monthly EPF contribution of $monthly2, over $years years" +
                (maturity?.let { ", is projected to grow to $it." } ?: ".")
        }
        "SSY" -> {
            val yearly = money(fields.num("yearlyDeposit")) ?: return null
            val age = fields["girlAgeAtOpening"] ?: return null
            "Depositing $yearly every year for a Sukanya Samriddhi account opened at age $age" +
                (maturity?.let { ", this is projected to mature to $it." } ?: ".")
        }
        "NPS" -> {
            val monthly = money(fields.num("monthlyContribution")) ?: return null
            val age = fields["currentAge"] ?: return null
            "Contributing $monthly every month from age $age" +
                (maturity?.let { ", this NPS corpus is projected to reach $it at retirement." } ?: ".")
        }
        else -> null
    }
}
