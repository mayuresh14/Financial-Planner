package com.financeplanner.app.ui.calculations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.repository.SavedCalculationRepository
import com.financeplanner.app.domain.model.SavedCalculation
import com.financeplanner.app.domain.model.SavedCalculationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedCalculationsUiState(
    val items: List<SavedCalculation> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: SavedCalculationType? = null,
    val selectedItem: SavedCalculation? = null,
    val pendingDelete: SavedCalculation? = null
) {
    val availableTypes: List<SavedCalculationType>
        get() = items.map { it.type }.distinct()

    val filteredItems: List<SavedCalculation>
        get() = items
            .filter { typeFilter == null || it.type == typeFilter }
            .filter { searchQuery.isBlank() || it.customName.contains(searchQuery, ignoreCase = true) }
}

@HiltViewModel
class SavedCalculationsViewModel @Inject constructor(
    private val repository: SavedCalculationRepository,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<SavedCalculation?>(null)
    private val _pendingDelete = MutableStateFlow<SavedCalculation?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<SavedCalculationType?>(null)

    val uiState: StateFlow<SavedCalculationsUiState> = kotlinx.coroutines.flow.combine(
        repository.observeAll(), _selectedItem, _pendingDelete, _searchQuery, _typeFilter
    ) { items, selected, pendingDelete, searchQuery, typeFilter ->
        SavedCalculationsUiState(
            items = items,
            searchQuery = searchQuery,
            typeFilter = typeFilter,
            selectedItem = selected,
            pendingDelete = pendingDelete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedCalculationsUiState())

    init {
        analytics.logScreenOpened(SCREEN_NAME)
    }

    fun onSearchQueryChange(query: String) {
        if (query.isNotBlank() && _searchQuery.value.isBlank()) {
            analytics.logSearchUsed(SCREEN_NAME)
        }
        _searchQuery.value = query
    }

    fun onTypeFilterChange(type: SavedCalculationType?) {
        if (type != null) analytics.logFilterApplied(SCREEN_NAME, type.name)
        _typeFilter.value = type
    }

    fun onItemClicked(item: SavedCalculation) {
        analytics.logSavedItemOpened(SCREEN_NAME, item.type.name)
        _selectedItem.value = item
    }

    fun onDetailDismissed() {
        _selectedItem.value = null
    }

    /** Called right before navigating to the edit route, so it's logged exactly once per edit. */
    fun onEditRequested(item: SavedCalculation) {
        analytics.logSavedItemEdited(SCREEN_NAME, item.type.name)
    }

    fun onDeleteRequested(item: SavedCalculation) {
        _pendingDelete.value = item
    }

    fun onDeleteCancelled() {
        _pendingDelete.value = null
    }

    fun onDeleteConfirmed() {
        val item = _pendingDelete.value ?: return
        analytics.logSavedItemDeleted(SCREEN_NAME, item.type.name)
        viewModelScope.launch { repository.delete(item) }
        _pendingDelete.value = null
        _selectedItem.value = null
    }

    private companion object {
        const val SCREEN_NAME = "calculations"
    }
}
