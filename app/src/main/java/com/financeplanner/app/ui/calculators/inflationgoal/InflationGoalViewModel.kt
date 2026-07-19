package com.financeplanner.app.ui.calculators.inflationgoal

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.InflationGoalInput
import com.financeplanner.app.domain.model.InflationGoalResult
import com.financeplanner.app.domain.usecase.CalculateInflationGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface InflationGoalValidationError {
    data object InvalidInput : InflationGoalValidationError
    data class InvalidValue(val rawMessage: String?) : InflationGoalValidationError
}

data class InflationGoalUiState(
    val currentCost: String = "",
    val inflationPercent: String = "6",
    val years: String = "10",
    val result: InflationGoalResult? = null,
    val error: InflationGoalValidationError? = null
)

@HiltViewModel
class InflationGoalViewModel @Inject constructor(
    private val calculateInflationGoal: CalculateInflationGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InflationGoalUiState())
    val uiState: StateFlow<InflationGoalUiState> = _uiState.asStateFlow()

    fun onCurrentCostChange(value: String) {
        _uiState.value = _uiState.value.copy(currentCost = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }
    fun onYearsChange(value: String) {
        _uiState.value = _uiState.value.copy(years = value, error = null)
    }

    fun calculate() {
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
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = InflationGoalValidationError.InvalidValue(e.message), result = null)
        }
    }
}
