package com.financeplanner.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financeplanner.app.R
import com.financeplanner.app.ui.calculations.SavedCalculationsScreen
import com.financeplanner.app.ui.home.CalculatorsScreen
import com.financeplanner.app.ui.home.DashboardScreen
import com.financeplanner.app.ui.investments.MyInvestmentsScreen

private data class BottomTab(val route: String, @androidx.annotation.StringRes val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(TabRoutes.DASHBOARD, R.string.tab_home, Icons.Filled.Home),
    BottomTab(TabRoutes.CALCULATORS, R.string.tab_calculators, Icons.Filled.List),
    BottomTab(TabRoutes.INVESTMENTS, R.string.tab_investments, Icons.Filled.Star),
    BottomTab(TabRoutes.CALCULATIONS, R.string.tab_calculations, Icons.Filled.Edit)
)

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The bottom-nav shell: Home (dashboard) / Calculators / Investments /
 * Calculations. This is registered as ONE route (Routes.MAIN) in the outer
 * NavHost — individual calculator screens push on top of the OUTER NavHost
 * (via [outerNavController]), so they render full-screen without this bottom
 * bar, while switching between these tabs never leaves this inner NavHost.
 * Investments = real accounts (FD/RD/PPF/EPF/SSY/NPS, has dates); Calculations
 * = "what-if" scenario snapshots (SIP/Lumpsum/Goal-based SIP, no dates).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(outerNavController: NavHostController) {
    val tabNavController = rememberNavController()
    val pendingTabNavigator = hiltViewModel<PendingTabNavigatorHolder>().navigator

    LaunchedEffect(Unit) {
        pendingTabNavigator.pendingTab.collect { route -> tabNavController.navigateToTab(route) }
    }

    Scaffold(
        // Only reserve space for the bottom bar here — each tab screen has its
        // own Scaffold/TopAppBar that already handles the top status-bar inset,
        // so also applying the outer Scaffold's top inset double-pads every
        // screen (this was the "too much space at top" bug).
        bottomBar = {
            val backStackEntry by tabNavController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { tabNavController.navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = if (isSelected) {
                                    MaterialTheme.typography.labelMedium
                                } else {
                                    MaterialTheme.typography.labelSmall
                                }
                            )
                        },
                        // Defaults lean on secondaryContainer for the selected pill, which
                        // read as too pale against the (now themed) nav bar background.
                        // Full-strength `primary` was tried next, but for presets whose
                        // primary is itself a saturated red/orange (Sunset, Crimson) that
                        // made the whole nav bar read as an alarming wall of red.
                        // primaryContainer is the middle ground: still clearly the theme's
                        // color and distinct from the nav bar background, without being
                        // the single most intense color in the app pinned to the tab bar.
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = TabRoutes.DASHBOARD,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(TabRoutes.DASHBOARD) {
                DashboardScreen(onNavigateToInvestments = { tabNavController.navigateToTab(TabRoutes.INVESTMENTS) })
            }
            composable(TabRoutes.CALCULATORS) {
                CalculatorsScreen(onNavigateToCalculator = { route -> outerNavController.navigate(route) })
            }
            composable(TabRoutes.INVESTMENTS) {
                MyInvestmentsScreen(
                    onBack = null,
                    onAddInvestment = { route -> outerNavController.navigate(route) },
                    onEditInvestment = { route -> outerNavController.navigate(route) }
                )
            }
            composable(TabRoutes.CALCULATIONS) {
                SavedCalculationsScreen(
                    onEditCalculation = { route -> outerNavController.navigate(route) }
                )
            }
        }
    }
}
