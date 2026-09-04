package com.financeplanner.app.ui.common

import java.text.NumberFormat
import java.util.Locale

/**
 * Friendly labels for known saved-field keys (English only for now — these
 * are raw internal field names from each calculator's input form, not a
 * user-facing vocabulary that's been localized elsewhere). Falls back to a
 * camelCase splitter for anything not in the map, so new fields never show
 * raw as "tenureMonths". Shared by MyInvestmentsScreen and
 * SavedCalculationsScreen, since both display decoded [SavedInvestmentJson]
 * field maps.
 */
private val fieldLabelOverrides = mapOf(
    "startDate" to "Start Date",
    "endDate" to "End Date",
    "tenureMonths" to "Tenure (months)",
    "annualRatePercent" to "Annual Rate (%)",
    "inflationPercent" to "Inflation (%)",
    "principal" to "Principal",
    "monthlyDeposit" to "Monthly Deposit",
    "expectedReturnPercent" to "Expected Return (%)",
    "durationYears" to "Duration (years)",
    "yearlyContribution" to "Yearly Contribution",
    "interestRatePercent" to "Interest Rate (%)",
    "basicMonthlySalary" to "Basic Monthly Salary",
    "employeeContributionPercent" to "Employee Contribution (%)",
    "employerContributionPercent" to "Employer Contribution (%)",
    "girlAgeAtOpening" to "Girl's Age at Opening",
    "yearlyDeposit" to "Yearly Deposit",
    "monthlyContribution" to "Monthly Contribution",
    "currentAge" to "Current Age",
    "annuityPercent" to "Annuity (%)",
    "goalName" to "Goal Name",
    "goalType" to "Goal Type",
    "targetAmount" to "Target Amount",
    "monthlyAmount" to "Monthly Amount",
    "frequency" to "Frequency",
    "stepUpMode" to "Step-up Mode",
    "stepUpPercent" to "Step-up (%)",
    "stepUpFixedAmount" to "Step-up Fixed Amount",
    "expenseRatioPercent" to "Expense Ratio (%)",
    "initialLumpsum" to "Initial Lumpsum"
)

/** Fields that exist purely for machine use, or are surfaced elsewhere as a dedicated badge
 * (assetCategory — see MyInvestmentsScreen's type/asset-category label) — never shown in the
 * generic field-by-field details list. */
val machineOnlyFieldKeys = setOf(
    "endDateMillis",
    "startDateMillis",
    "assetCategory",
    // Mode/frequency selector fields — never shown on their own since a blank step-up or
    // salary-vs-flat value would otherwise look like an intentionally chosen option; the
    // relevant value field (already labeled distinctly) is shown instead when non-blank.
    "stepUpMode",
    "contributionMode",
    "contributionFrequency"
)

fun formatFieldLabel(key: String): String = fieldLabelOverrides[key] ?: key
    .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
    .replaceFirstChar { it.uppercase() }

private val indianNumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }

/** Adds Indian-style comma grouping to a raw numeric field value (e.g. "500000" -> "5,00,000");
 * an ALL_CAPS_WITH_UNDERSCORES enum name (e.g. "MUTUAL_FUND") is title-cased instead; anything
 * else passes through unchanged. */
fun formatFieldValue(raw: String): String {
    raw.toDoubleOrNull()?.let { return indianNumberFormat.format(it) }
    if (raw.isNotEmpty() && raw.all { it.isUpperCase() || it == '_' || it.isDigit() }) {
        return raw.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
    }
    return raw
}
