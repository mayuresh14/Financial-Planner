package com.financeplanner.app.ui.calculators.inflationgoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.InflationGoalInput
import com.financeplanner.app.domain.model.InflationGoalResult
import com.financeplanner.app.domain.usecase.CalculateInflationGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InflationGoalValidationError {
    data object InvalidInput : InflationGoalValidationError
    data class InvalidValue(val rawMessage: String?) : InflationGoalValidationError
}

data class InflationGoalUiState(
    val currentCost: String = "",
    val inflationPercent: String = "6",
    val years: String = "",
    val result: InflationGoalResult? = null,
    val error: InflationGoalValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class InflationGoalViewModel @Inject constructor(
    private val calculateInflationGoal: CalculateInflationGoalUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(InflationGoalUiState())
    val uiState: StateFlow<InflationGoalUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(inflationPercent = prefs.defaultInflationPercent.toString())
        }
    }

    fun onCurrentCostChange(value: String) {
        _uiState.value = _uiState.value.copy(currentCost = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }
    fun onYearsChange(value: String) {
        _uiState.value = _uiState.value.copy(years = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    fun onSaveClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        _uiState.value = _uiState.value.copy(showComingSoonSheet = true)
    }

    fun onComingSoonDismissed() {
        _uiState.value = _uiState.value.copy(showComingSoonSheet = false)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        val state = _uiState.value
        val currentCost = state.currentCost.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val years = state.years.toIntOrNull()

        if (currentCost == null || inflation == null || years == null) {
            _uiState.value = state.copy(error = InflationGoalValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateInflationGoal(InflationGoalInput(currentCost, inflation, years))
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = InflationGoalValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "inflation_goal"
    }
}
