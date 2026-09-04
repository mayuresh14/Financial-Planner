package com.financeplanner.app.ui.common

import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType

/** One slice of the portfolio-mix chart: a type and its share of [InvestmentAnalysis.totalProjectedValue]. */
data class TypeValueSlice(
    val type: SavedInvestmentType,
    val value: Double
)

/** One slice of the asset-class chart — only SIP/Lumpsum entries carry an asset category. */
data class AssetValueSlice(
    val category: AssetCategory,
    val value: Double
)

/** An FD/RD entry maturing soon, for the "upcoming maturities" list. */
data class UpcomingMaturity(
    val item: SavedInvestment,
    val maturityDateMillis: Long
)

data class InvestmentAnalysis(
    val totalMonthlyCommitment: Double,
    val totalLumpSumInvested: Double,
    val totalProjectedValue: Double,
    val typeBreakdown: List<TypeValueSlice>,
    val assetCategoryBreakdown: List<AssetValueSlice>,
    val upcomingMaturities: List<UpcomingMaturity>
) {
    companion object {
        val EMPTY = InvestmentAnalysis(0.0, 0.0, 0.0, emptyList(), emptyList(), emptyList())
    }
}

/** SIP/Lumpsum only — other types don't carry an asset category. Null if unset (saved before this feature existed). */
fun SavedInvestment.assetCategoryOrNull(): AssetCategory? {
    if (type != SavedInvestmentType.SIP && type != SavedInvestmentType.LUMPSUM) return null
    val raw = SavedInvestmentJson.decode(detailsJson)["assetCategory"] ?: return null
    return runCatching { AssetCategory.valueOf(raw) }.getOrNull()
}

private const val UPCOMING_MATURITY_WINDOW_DAYS = 90L
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Goal-based SIP is excluded from value/mix totals: its [SavedInvestment.lastComputedValue]
 * holds the *required monthly SIP*, not a maturity value, since that's what the
 * calculator solves for — mixing it in would corrupt the projected-value sum.
 */
private val VALUE_BEARING_TYPES = setOf(
    SavedInvestmentType.FD, SavedInvestmentType.RD, SavedInvestmentType.PPF,
    SavedInvestmentType.EPF, SavedInvestmentType.SSY, SavedInvestmentType.NPS,
    SavedInvestmentType.SIP, SavedInvestmentType.LUMPSUM
)

fun computeInvestmentAnalysis(items: List<SavedInvestment>, nowMillis: Long = System.currentTimeMillis()): InvestmentAnalysis {
    if (items.isEmpty()) return InvestmentAnalysis.EMPTY

    var totalMonthly = 0.0
    var totalLumpSum = 0.0
    val valueByType = linkedMapOf<SavedInvestmentType, Double>()
    val valueByAssetCategory = linkedMapOf<AssetCategory, Double>()
    val maturities = mutableListOf<UpcomingMaturity>()
    val windowEnd = nowMillis + UPCOMING_MATURITY_WINDOW_DAYS * MILLIS_PER_DAY

    items.forEach { item ->
        val fields = SavedInvestmentJson.decode(item.detailsJson)
        totalMonthly += monthlyCommitmentOf(item.type, fields)
        totalLumpSum += lumpSumOf(item.type, fields)

        if (item.type in VALUE_BEARING_TYPES) {
            val value = item.lastComputedValue ?: 0.0
            valueByType[item.type] = (valueByType[item.type] ?: 0.0) + value

            item.assetCategoryOrNull()?.let { category ->
                valueByAssetCategory[category] = (valueByAssetCategory[category] ?: 0.0) + value
            }
        }

        fields["endDateMillis"]?.toLongOrNull()?.let { maturityMillis ->
            if (maturityMillis in nowMillis..windowEnd) {
                maturities += UpcomingMaturity(item, maturityMillis)
            }
        }
    }

    val totalProjectedValue = valueByType.values.sum()
    val typeBreakdown = valueByType
        .filterValues { it > 0.0 }
        .map { (type, value) -> TypeValueSlice(type, value) }
        .sortedByDescending { it.value }
    val assetCategoryBreakdown = valueByAssetCategory
        .filterValues { it > 0.0 }
        .map { (category, value) -> AssetValueSlice(category, value) }
        .sortedByDescending { it.value }

    return InvestmentAnalysis(
        totalMonthlyCommitment = totalMonthly,
        totalLumpSumInvested = totalLumpSum,
        totalProjectedValue = totalProjectedValue,
        typeBreakdown = typeBreakdown,
        assetCategoryBreakdown = assetCategoryBreakdown,
        upcomingMaturities = maturities.sortedBy { it.maturityDateMillis }
    )
}

private fun monthlyCommitmentOf(type: SavedInvestmentType, fields: Map<String, String>): Double = when (type) {
    SavedInvestmentType.SIP -> fields["monthlyAmount"]?.toDoubleOrNull() ?: 0.0
    SavedInvestmentType.RD -> fields["monthlyDeposit"]?.toDoubleOrNull() ?: 0.0
    SavedInvestmentType.NPS -> fields["monthlyContribution"]?.toDoubleOrNull() ?: 0.0
    SavedInvestmentType.PPF -> {
        val monthly = fields["monthlyContribution"]?.toDoubleOrNull()
        if (fields["contributionFrequency"] == "MONTHLY" && monthly != null) {
            monthly
        } else {
            (fields["yearlyContribution"]?.toDoubleOrNull() ?: 0.0) / 12.0
        }
    }
    SavedInvestmentType.SSY -> (fields["yearlyDeposit"]?.toDoubleOrNull() ?: 0.0) / 12.0
    SavedInvestmentType.EPF -> {
        val flat = fields["flatMonthlyContribution"]?.toDoubleOrNull()
        if (fields["contributionMode"] == "FLAT_AMOUNT" && flat != null) {
            flat
        } else {
            val salary = fields["basicMonthlySalary"]?.toDoubleOrNull() ?: 0.0
            val employeePercent = fields["employeeContributionPercent"]?.toDoubleOrNull() ?: 0.0
            val employerPercent = fields["employerContributionPercent"]?.toDoubleOrNull() ?: 0.0
            salary * (employeePercent + employerPercent) / 100.0
        }
    }
    SavedInvestmentType.FD, SavedInvestmentType.LUMPSUM, SavedInvestmentType.GOAL_BASED_SIP -> 0.0
}

private fun lumpSumOf(type: SavedInvestmentType, fields: Map<String, String>): Double = when (type) {
    SavedInvestmentType.FD, SavedInvestmentType.LUMPSUM -> fields["principal"]?.toDoubleOrNull() ?: 0.0
    else -> 0.0
}
