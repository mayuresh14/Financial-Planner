package com.financeplanner.app.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.InvestmentsViewMode
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.ui.common.assetCategoryOrNull
import com.financeplanner.app.ui.navigation.PendingInvestmentDetailNavigator
import com.financeplanner.app.ui.navigation.PendingInvestmentsFilter
import com.financeplanner.app.ui.navigation.PendingInvestmentsFilterNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row of the Summary view: how many saved items of [type] there are and their combined value.
 * [totalValue] is null for Goal-based SIP, whose stored value is a required-SIP figure, not a maturity value. */
data class TypeSummaryRow(
    val type: SavedInvestmentType,
    val count: Int,
    val totalValue: Double?
)

data class MyInvestmentsUiState(
    val items: List<SavedInvestment> = emptyList(),
    val searchQuery: String = "",
    /** Empty means "All" — no type filtering applied. Otherwise OR'd together (any match). */
    val typeFilters: Set<SavedInvestmentType> = emptySet(),
    /** Same "empty = All" convention as [typeFilters]. */
    val assetCategoryFilters: Set<AssetCategory> = emptySet(),
    val selectedItem: SavedInvestment? = null,
    val pendingDelete: SavedInvestment? = null,
    val viewMode: InvestmentsViewMode = InvestmentsViewMode.LIST
) {
    /** All types actually present in [items] — the filter row only shows chips that would match something. */
    val availableTypes: List<SavedInvestmentType>
        get() = items.map { it.type }.distinct()

    /** Only SIP/Lumpsum carry an asset category — this row only appears once at least one is set. */
    val availableAssetCategories: List<AssetCategory>
        get() = items.mapNotNull { it.assetCategoryOrNull() }.distinct()

    val filteredItems: List<SavedInvestment>
        get() = items
            .filter { typeFilters.isEmpty() || it.type in typeFilters }
            .filter { assetCategoryFilters.isEmpty() || it.assetCategoryOrNull() in assetCategoryFilters }
            .filter { item ->
                searchQuery.isBlank() ||
                    item.customName.contains(searchQuery, ignoreCase = true) ||
                    item.institutionName?.contains(searchQuery, ignoreCase = true) == true
            }

    /** Breakdown for the Summary view — one row per type, richest first. Type/asset-category
     * filters are a List-view-only concept (never set while in Summary mode), so [filteredItems]
     * here effectively means "matching the search query" — letting search narrow both which
     * sections show and what's inside them when expanded. */
    val typeSummaryRows: List<TypeSummaryRow>
        get() = filteredItems.groupBy { it.type }
            .map { (type, itemsOfType) ->
                TypeSummaryRow(
                    type = type,
                    count = itemsOfType.size,
                    totalValue = if (type == SavedInvestmentType.GOAL_BASED_SIP) {
                        null
                    } else {
                        itemsOfType.sumOf { it.lastComputedValue ?: 0.0 }
                    }
                )
            }
            .sortedByDescending { it.totalValue ?: -1.0 }
}

@HiltViewModel
class MyInvestmentsViewModel @Inject constructor(
    private val repository: SavedInvestmentRepository,
    private val preferencesDataStore: AppPreferencesDataStore,
    pendingInvestmentsFilterNavigator: PendingInvestmentsFilterNavigator,
    pendingInvestmentDetailNavigator: PendingInvestmentDetailNavigator
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<SavedInvestment?>(null)
    private val _pendingDelete = MutableStateFlow<SavedInvestment?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _typeFilters = MutableStateFlow<Set<SavedInvestmentType>>(emptySet())
    private val _assetCategoryFilters = MutableStateFlow<Set<AssetCategory>>(emptySet())

    private val filtersFlow = combine(_searchQuery, _typeFilters, _assetCategoryFilters) { search, types, categories -> Triple(search, types, categories) }
    private val selectionFlow = combine(_selectedItem, _pendingDelete) { selected, pending -> selected to pending }
    private val viewModeFlow = preferencesDataStore.preferencesFlow.map { it.investmentsViewMode }

    val uiState: StateFlow<MyInvestmentsUiState> = combine(
        repository.observeAll(), filtersFlow, selectionFlow, viewModeFlow
    ) { items, filters, selection, viewMode ->
        MyInvestmentsUiState(
            items = items,
            searchQuery = filters.first,
            typeFilters = filters.second,
            assetCategoryFilters = filters.third,
            selectedItem = selection.first,
            pendingDelete = selection.second,
            viewMode = viewMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyInvestmentsUiState())

    init {
        // A Home dashboard pie-chart slice tap asked to land here pre-filtered — replace
        // whichever dimension it named with just that one selection and clear the other
        // dimension + any leftover search text, so the result is unambiguous.
        viewModelScope.launch {
            pendingInvestmentsFilterNavigator.pendingFilter.collect { filter ->
                _searchQuery.value = ""
                when (filter) {
                    is PendingInvestmentsFilter.ByType -> {
                        _typeFilters.value = setOf(filter.type)
                        _assetCategoryFilters.value = emptySet()
                    }
                    is PendingInvestmentsFilter.ByAssetCategory -> {
                        _assetCategoryFilters.value = setOf(filter.category)
                        _typeFilters.value = emptySet()
                    }
                }
            }
        }

        // A maturity-reminder notification tap asked to open one specific item's detail sheet.
        // Looked up directly via the repository rather than the (possibly not-yet-loaded)
        // `items` list, so it works correctly even on a cold start.
        viewModelScope.launch {
            pendingInvestmentDetailNavigator.pendingItemId.collect { id ->
                repository.getById(id)?.let { _selectedItem.value = it }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onViewModeToggled() {
        val next = if (uiState.value.viewMode == InvestmentsViewMode.LIST) InvestmentsViewMode.SUMMARY else InvestmentsViewMode.LIST
        viewModelScope.launch { preferencesDataStore.setInvestmentsViewMode(next) }
    }

    /** null clears to "All"; otherwise toggles that type's membership in the selection. */
    fun onTypeFilterToggle(type: SavedInvestmentType?) {
        _typeFilters.value = if (type == null) {
            emptySet()
        } else {
            _typeFilters.value.let { current -> if (type in current) current - type else current + type }
        }
    }

    /** null clears to "All"; otherwise toggles that category's membership in the selection. */
    fun onAssetCategoryFilterToggle(category: AssetCategory?) {
        _assetCategoryFilters.value = if (category == null) {
            emptySet()
        } else {
            _assetCategoryFilters.value.let { current -> if (category in current) current - category else current + category }
        }
    }

    fun onItemClicked(item: SavedInvestment) {
        _selectedItem.value = item
    }

    fun onDetailDismissed() {
        _selectedItem.value = null
    }

    fun onDeleteRequested(item: SavedInvestment) {
        _pendingDelete.value = item
    }

    fun onDeleteCancelled() {
        _pendingDelete.value = null
    }

    fun onDeleteConfirmed() {
        val item = _pendingDelete.value ?: return
        viewModelScope.launch {
            repository.delete(item)
        }
        _pendingDelete.value = null
        _selectedItem.value = null
    }
}
