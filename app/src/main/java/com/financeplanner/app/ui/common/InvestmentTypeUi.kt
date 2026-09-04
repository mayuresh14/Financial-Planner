package com.financeplanner.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.SavedInvestmentType

@Composable
fun investmentTypeLabel(type: SavedInvestmentType): String = when (type) {
    SavedInvestmentType.FD -> stringResource(R.string.investment_type_fd)
    SavedInvestmentType.RD -> stringResource(R.string.investment_type_rd)
    SavedInvestmentType.PPF -> stringResource(R.string.investment_type_ppf)
    SavedInvestmentType.EPF -> stringResource(R.string.investment_type_epf)
    SavedInvestmentType.SSY -> stringResource(R.string.calc_ssy)
    SavedInvestmentType.NPS -> stringResource(R.string.investment_type_nps)
    SavedInvestmentType.SIP -> stringResource(R.string.investment_type_sip)
    SavedInvestmentType.LUMPSUM -> stringResource(R.string.investment_type_lumpsum)
    SavedInvestmentType.GOAL_BASED_SIP -> stringResource(R.string.calc_goal_based_sip)
}

/**
 * Fixed per-type colors for portfolio-mix charts — deliberately independent of the
 * app's Material theme (which the user can randomize/swap), so a type's color stays
 * stable and distinguishable regardless of which theme preset is active. Chosen from
 * the Okabe-Ito colorblind-safe qualitative palette.
 */
fun investmentTypeChartColor(type: SavedInvestmentType): Color = when (type) {
    SavedInvestmentType.FD -> Color(0xFF0072B2)
    SavedInvestmentType.RD -> Color(0xFFD55E00)
    SavedInvestmentType.PPF -> Color(0xFF009E73)
    SavedInvestmentType.EPF -> Color(0xFFCC79A7)
    SavedInvestmentType.SSY -> Color(0xFFE69F00)
    SavedInvestmentType.NPS -> Color(0xFF56B4E9)
    SavedInvestmentType.SIP -> Color(0xFF999999)
    SavedInvestmentType.LUMPSUM -> Color(0xFFB8860B)
    SavedInvestmentType.GOAL_BASED_SIP -> Color(0xFF999999)
}
