package com.financeplanner.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.financeplanner.app.ui.home.HomeScreen

/**
 * App-wide navigation graph. Home is the start destination — it lists every
 * calculator (see ui/home/CalculatorCatalog.kt) and routes to built ones.
 * All 14 Phase 1 calculators are now wired in.
 */
object Routes {
    const val HOME = "home"
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
}

@Composable
fun FinancePlannerNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToCalculator = { route -> navController.navigate(route) }
            )
        }
        composable(Routes.SIP) { SipScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.LUMPSUM) { LumpsumScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.TENURE) { TenureScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INFLATION_GOAL) { InflationGoalScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.GOAL_BASED_SIP) { GoalBasedSipScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SIP_VS_LUMPSUM) { SipVsLumpsumScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PPF) { PpfScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EPF) { EpfScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SSY) { SsyScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.NPS) { NpsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EMI) { EmiScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.STP) { StpScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SWP) { SwpScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FIRE) { FireScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FD) { FdScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.RD) { RdScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SMOKE) { SmokeScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ALCOHOL) { AlcoholScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EATING_OUT) { EatingOutScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.GRATUITY) { GratuityScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ASSET_ALLOCATION) { AssetAllocationScreen(onBack = { navController.popBackStack() }) }
    }
}
