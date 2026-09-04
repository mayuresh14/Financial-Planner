package com.financeplanner.app.ui.navigation

import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.SavedInvestmentType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Which dimension to pre-filter the Investments tab by, requested from a pie-chart slice tap
 * on the Home dashboard. */
sealed interface PendingInvestmentsFilter {
    data class ByType(val type: SavedInvestmentType) : PendingInvestmentsFilter
    data class ByAssetCategory(val category: AssetCategory) : PendingInvestmentsFilter
}

/**
 * Same hand-off problem and same fix as [PendingTabNavigator]: MyInvestmentsScreen is a
 * NavHost tab destination, so its ViewModel may not exist yet (or may briefly not be
 * collecting) at the moment a Home dashboard chart tap requests a filter — a Channel holds
 * the request until MyInvestmentsViewModel actually collects it, however late that is.
 */
@Singleton
class PendingInvestmentsFilterNavigator @Inject constructor() {
    private val _pendingFilter = Channel<PendingInvestmentsFilter>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pendingFilter: Flow<PendingInvestmentsFilter> = _pendingFilter.receiveAsFlow()

    fun requestFilter(filter: PendingInvestmentsFilter) {
        _pendingFilter.trySend(filter)
    }
}
