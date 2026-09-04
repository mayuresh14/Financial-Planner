package com.financeplanner.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.ui.common.InvestmentAnalysis
import com.financeplanner.app.ui.common.computeInvestmentAnalysis
import com.financeplanner.app.ui.navigation.PendingInvestmentsFilter
import com.financeplanner.app.ui.navigation.PendingInvestmentsFilterNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalInvestments: Int = 0,
    val analysis: InvestmentAnalysis = InvestmentAnalysis.EMPTY
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: SavedInvestmentRepository,
    private val pendingInvestmentsFilterNavigator: PendingInvestmentsFilterNavigator
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.observeAll()
        .map { items ->
            DashboardUiState(
                totalInvestments = items.size,
                analysis = computeInvestmentAnalysis(items)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    /** A portfolio-mix slice was tapped — hand off which type to pre-filter the
     * Investments tab by; the caller still has to actually navigate there. */
    fun onTypeSliceClicked(type: SavedInvestmentType) {
        viewModelScope.launch { pendingInvestmentsFilterNavigator.requestFilter(PendingInvestmentsFilter.ByType(type)) }
    }

    /** Same, for an asset-class-mix slice. */
    fun onAssetCategorySliceClicked(category: AssetCategory) {
        viewModelScope.launch {
            pendingInvestmentsFilterNavigator.requestFilter(PendingInvestmentsFilter.ByAssetCategory(category))
        }
    }
}
