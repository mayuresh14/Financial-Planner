package com.financeplanner.app.ui.home

import androidx.annotation.StringRes
import com.financeplanner.app.R
import com.financeplanner.app.ui.navigation.Routes

/**
 * One entry per calculator card on the home screen. All 14 Phase 1
 * calculators are now built and navigable.
 */
data class CalculatorCatalogItem(
    @StringRes val titleRes: Int,
    val badgeText: String,
    val route: String?
)

object CalculatorCatalog {
    val items = listOf(
        CalculatorCatalogItem(R.string.calc_sip, "SIP", Routes.SIP),
        CalculatorCatalogItem(R.string.calc_lumpsum, "LS", Routes.LUMPSUM),
        CalculatorCatalogItem(R.string.calc_sip_vs_lumpsum, "VS", Routes.SIP_VS_LUMPSUM),
        CalculatorCatalogItem(R.string.calc_goal_based_sip, "GS", Routes.GOAL_BASED_SIP),
        CalculatorCatalogItem(R.string.calc_tenure, "TN", Routes.TENURE),
        CalculatorCatalogItem(R.string.calc_ppf, "PPF", Routes.PPF),
        CalculatorCatalogItem(R.string.calc_epf, "EPF", Routes.EPF),
        CalculatorCatalogItem(R.string.calc_ssy, "SSY", Routes.SSY),
        CalculatorCatalogItem(R.string.calc_nps, "NPS", Routes.NPS),
        CalculatorCatalogItem(R.string.calc_emi, "EMI", Routes.EMI),
        CalculatorCatalogItem(R.string.calc_stp, "STP", Routes.STP),
        CalculatorCatalogItem(R.string.calc_swp, "SWP", Routes.SWP),
        CalculatorCatalogItem(R.string.calc_inflation_goal, "INF", Routes.INFLATION_GOAL),
        CalculatorCatalogItem(R.string.calc_fire, "FIRE", Routes.FIRE)
    )
}
