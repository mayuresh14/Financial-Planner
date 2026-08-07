package com.financeplanner.app.ui.home

import androidx.annotation.StringRes
import com.financeplanner.app.R
import com.financeplanner.app.ui.navigation.Routes

/** Groups calculators into home-screen carousels. Order here is display order. */
enum class CalculatorCategory(@StringRes val titleRes: Int) {
    INVESTMENT(R.string.category_investment),
    SAVINGS(R.string.category_savings),
    PLANNING(R.string.category_planning),
    GOVERNMENT_SCHEMES(R.string.category_government_schemes),
    LOAN(R.string.category_loan),
    LIFESTYLE(R.string.category_lifestyle)
}

/**
 * One entry per calculator card on the home screen. All 14 Phase 1
 * calculators are now built and navigable.
 */
data class CalculatorCatalogItem(
    @StringRes val titleRes: Int,
    val badgeText: String,
    val route: String?,
    val category: CalculatorCategory
)

object CalculatorCatalog {
    val items = listOf(
        CalculatorCatalogItem(R.string.calc_sip, "SIP", Routes.SIP, CalculatorCategory.INVESTMENT),
        CalculatorCatalogItem(R.string.calc_lumpsum, "LS", Routes.LUMPSUM, CalculatorCategory.INVESTMENT),
        CalculatorCatalogItem(R.string.calc_sip_vs_lumpsum, "VS", Routes.SIP_VS_LUMPSUM, CalculatorCategory.INVESTMENT),
        CalculatorCatalogItem(R.string.calc_stp, "STP", Routes.STP, CalculatorCategory.INVESTMENT),
        CalculatorCatalogItem(R.string.calc_fd, "FD", Routes.FD, CalculatorCategory.SAVINGS),
        CalculatorCatalogItem(R.string.calc_rd, "RD", Routes.RD, CalculatorCategory.SAVINGS),
        CalculatorCatalogItem(R.string.calc_ppf, "PPF", Routes.PPF, CalculatorCategory.GOVERNMENT_SCHEMES),
        CalculatorCatalogItem(R.string.calc_epf, "EPF", Routes.EPF, CalculatorCategory.GOVERNMENT_SCHEMES),
        CalculatorCatalogItem(R.string.calc_ssy, "SSY", Routes.SSY, CalculatorCategory.GOVERNMENT_SCHEMES),
        CalculatorCatalogItem(R.string.calc_nps, "NPS", Routes.NPS, CalculatorCategory.GOVERNMENT_SCHEMES),
        CalculatorCatalogItem(R.string.calc_gratuity, "GRT", Routes.GRATUITY, CalculatorCategory.GOVERNMENT_SCHEMES),
        CalculatorCatalogItem(R.string.calc_fire, "FIRE", Routes.FIRE, CalculatorCategory.PLANNING),
        CalculatorCatalogItem(R.string.calc_goal_based_sip, "GS", Routes.GOAL_BASED_SIP, CalculatorCategory.PLANNING),
        CalculatorCatalogItem(R.string.calc_swp, "SWP", Routes.SWP, CalculatorCategory.PLANNING),
        CalculatorCatalogItem(R.string.calc_inflation_goal, "INF", Routes.INFLATION_GOAL, CalculatorCategory.PLANNING),
        CalculatorCatalogItem(R.string.calc_asset_allocation, "AA", Routes.ASSET_ALLOCATION, CalculatorCategory.PLANNING),
        CalculatorCatalogItem(R.string.calc_tenure, "TN", Routes.TENURE, CalculatorCategory.PLANNING),
        CalculatorCatalogItem(R.string.calc_emi, "EMI", Routes.EMI, CalculatorCategory.LOAN),
        CalculatorCatalogItem(R.string.calc_smoke, "SMK", Routes.SMOKE, CalculatorCategory.LIFESTYLE),
        CalculatorCatalogItem(R.string.calc_alcohol, "ALC", Routes.ALCOHOL, CalculatorCategory.LIFESTYLE),
        CalculatorCatalogItem(R.string.calc_eating_out, "EAT", Routes.EATING_OUT, CalculatorCategory.LIFESTYLE)
    )

    val itemsByCategory: List<Pair<CalculatorCategory, List<CalculatorCatalogItem>>> =
        CalculatorCategory.entries.map { category -> category to items.filter { it.category == category } }
}
