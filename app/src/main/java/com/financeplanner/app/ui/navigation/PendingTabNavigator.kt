package com.financeplanner.app.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

object TabRoutes {
    const val DASHBOARD = "tab_dashboard"
    const val CALCULATORS = "tab_calculators"
    const val INVESTMENTS = "tab_investments"
    const val CALCULATIONS = "tab_calculations"
}

/**
 * Calculator screens live in the OUTER NavHost; the bottom-nav tabs live in
 * an INNER NavHost nested inside MainTabsScreen. When a calculator finishes
 * saving and pops back to Routes.MAIN, it needs to also tell the inner
 * NavHost which tab to land on (e.g. Investments after saving an FD) — this
 * app-scoped singleton is the hand-off point between the two, since they
 * don't share a NavBackStackEntry to pass state through directly.
 *
 * Backed by a [Channel], not a SharedFlow: MainTabsScreen is a NavHost
 * destination, so Compose Navigation disposes its composition (and cancels
 * its collecting LaunchedEffect) the moment a calculator screen is pushed on
 * top — which is exactly when a save (and requestTab call) happens. A
 * SharedFlow with replay = 0 drops an emission with no active collector, so
 * the tab switch silently never happened. A Channel holds the pending route
 * until it's actually received, however late that collector reattaches.
 */
@Singleton
class PendingTabNavigator @Inject constructor() {
    private val _pendingTab = Channel<String>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pendingTab: Flow<String> = _pendingTab.receiveAsFlow()

    fun requestTab(route: String) {
        _pendingTab.trySend(route)
    }
}

/** Thin ViewModel wrapper — [PendingTabNavigator] is a plain singleton, and
 * only ViewModels are retrievable via hiltViewModel() from a Composable. */
@HiltViewModel
class PendingTabNavigatorHolder @Inject constructor(
    val navigator: PendingTabNavigator
) : ViewModel()
