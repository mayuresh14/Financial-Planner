package com.financeplanner.app.ui.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Same hand-off problem and same fix as [PendingTabNavigator]/[PendingInvestmentsFilterNavigator]:
 * a maturity-reminder notification tap asks to open one specific saved item's detail sheet on
 * the Investments tab, but MyInvestmentsViewModel may not exist yet (cold start) or may not be
 * collecting yet at that moment — a Channel holds the request until it actually is.
 */
@Singleton
class PendingInvestmentDetailNavigator @Inject constructor() {
    private val _pendingItemId = Channel<Long>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pendingItemId: Flow<Long> = _pendingItemId.receiveAsFlow()

    fun requestDetail(id: Long) {
        _pendingItemId.trySend(id)
    }
}
