package com.financeplanner.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.financeplanner.app.ui.calculators.alcohol.AlcoholScreen
import com.financeplanner.app.ui.calculators.assetallocation.AssetAllocationScreen
import com.financeplanner.app.ui.calculators.eatingout.EatingOutScreen
import com.financeplanner.app.ui.calculators.emi.EmiScreen
import com.financeplanner.app.ui.calculators.epf.EpfScreen
import com.financeplanner.app.ui.calculators.fd.FdScreen
import com.financeplanner.app.ui.calculators.fire.FireScreen
import com.financeplanner.app.ui.calculators.goalbasedsip.GoalBasedSipScreen
import com.financeplanner.app.ui.calculators.gratuity.GratuityScreen
import com.financeplanner.app.ui.calculators.inflationgoal.InflationGoalScreen
import com.financeplanner.app.ui.calculators.lumpsum.LumpsumScreen
import com.financeplanner.app.ui.calculators.nps.NpsScreen
import com.financeplanner.app.ui.calculators.ppf.PpfScreen
import com.financeplanner.app.ui.calculators.rd.RdScreen
import com.financeplanner.app.ui.calculators.sip.SipScreen
import com.financeplanner.app.ui.calculators.smoke.SmokeScreen
import com.financeplanner.app.ui.calculators.sipvslumpsum.SipVsLumpsumScreen
import com.financeplanner.app.ui.calculators.ssy.SsyScreen
import com.financeplanner.app.ui.calculators.stp.StpScreen
import com.financeplanner.app.ui.calculators.swp.SwpScreen
import com.financeplanner.app.ui.calculators.tenure.TenureScreen

/**
 * App-wide navigation graph. Routes.MAIN is the start destination — it hosts
 * the bottom-nav shell (Home/Calculators/Investments, see MainTabsScreen).
 * Individual calculator screens are separate routes here so they push
 * full-screen on top of the bottom nav rather than living inside it.
 */
object Routes {
    const val MAIN = "main"
    const val SIP = "sip"
    const val LUMPSUM = "lumpsum"
    const val TENURE = "tenure"
    const val INFLATION_GOAL = "inflation_goal"
    const val GOAL_BASED_SIP = "goal_based_sip"
    const val SIP_VS_LUMPSUM = "sip_vs_lumpsum"
    const val PPF = "ppf"
    const val EPF = "epf"
    const val SSY = "ssy"
    const val NPS = "nps"
    const val EMI = "emi"
    const val STP = "stp"
    const val SWP = "swp"
    const val FIRE = "fire"
    const val FD = "fd"
    const val RD = "rd"
    const val SMOKE = "smoke"
    const val ALCOHOL = "alcohol"
    const val EATING_OUT = "eating_out"
    const val GRATUITY = "gratuity"
    const val ASSET_ALLOCATION = "asset_allocation"

    /** Nav-arg key used by every editable calculator's optional "edit an existing saved item" route. */
    const val EDIT_ITEM_ID_ARG = "itemId"
    const val NO_EDIT_ITEM_ID = -1L

    /** For SIP/Lumpsum/Goal-based SIP, which table the item being edited lives in — those three can be saved as either. */
    const val EDIT_ITEM_KIND_ARG = "itemKind"
    const val ITEM_KIND_INVESTMENT = "investment"
    const val ITEM_KIND_CALCULATION = "calculation"

    /** Route pattern for a calculator that supports editing a saved item, e.g. "fd?itemId={itemId}". */
    fun editablePattern(base: String) = "$base?$EDIT_ITEM_ID_ARG={$EDIT_ITEM_ID_ARG}"

    /** Route + id to navigate to when editing an existing saved item, e.g. "fd?itemId=42". */
    fun editRoute(base: String, id: Long) = "$base?$EDIT_ITEM_ID_ARG=$id"

    /** Route pattern for a calculator whose saves can be either kind, e.g. "sip?itemId={itemId}&itemKind={itemKind}". */
    fun editablePatternWithKind(base: String) = "$base?$EDIT_ITEM_ID_ARG={$EDIT_ITEM_ID_ARG}&$EDIT_ITEM_KIND_ARG={$EDIT_ITEM_KIND_ARG}"

    /** Route + id + kind to navigate to when editing an item that could be either an investment or a calculation. */
    fun editRouteWithKind(base: String, id: Long, kind: String) = "$base?$EDIT_ITEM_ID_ARG=$id&$EDIT_ITEM_KIND_ARG=$kind"
}

@Composable
fun FinancePlannerNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) { MainTabsScreen(outerNavController = navController) }
        composable(
            Routes.editablePatternWithKind(Routes.SIP),
            arguments = listOf(
                navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID },
                navArgument(Routes.EDIT_ITEM_KIND_ARG) { type = NavType.StringType; defaultValue = Routes.ITEM_KIND_CALCULATION }
            )
        ) { SipScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePatternWithKind(Routes.LUMPSUM),
            arguments = listOf(
                navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID },
                navArgument(Routes.EDIT_ITEM_KIND_ARG) { type = NavType.StringType; defaultValue = Routes.ITEM_KIND_CALCULATION }
            )
        ) { LumpsumScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.TENURE) { TenureScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INFLATION_GOAL) { InflationGoalScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePatternWithKind(Routes.GOAL_BASED_SIP),
            arguments = listOf(
                navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID },
                navArgument(Routes.EDIT_ITEM_KIND_ARG) { type = NavType.StringType; defaultValue = Routes.ITEM_KIND_CALCULATION }
            )
        ) { GoalBasedSipScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SIP_VS_LUMPSUM) { SipVsLumpsumScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePattern(Routes.PPF),
            arguments = listOf(navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID })
        ) { PpfScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePattern(Routes.EPF),
            arguments = listOf(navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID })
        ) { EpfScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePattern(Routes.SSY),
            arguments = listOf(navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID })
        ) { SsyScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePattern(Routes.NPS),
            arguments = listOf(navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID })
        ) { NpsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EMI) { EmiScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.STP) { StpScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SWP) { SwpScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FIRE) { FireScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePattern(Routes.FD),
            arguments = listOf(navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID })
        ) { FdScreen(onBack = { navController.popBackStack() }) }
        composable(
            Routes.editablePattern(Routes.RD),
            arguments = listOf(navArgument(Routes.EDIT_ITEM_ID_ARG) { type = NavType.LongType; defaultValue = Routes.NO_EDIT_ITEM_ID })
        ) { RdScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SMOKE) { SmokeScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ALCOHOL) { AlcoholScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EATING_OUT) { EatingOutScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.GRATUITY) { GratuityScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ASSET_ALLOCATION) { AssetAllocationScreen(onBack = { navController.popBackStack() }) }
    }
}
