package com.financeplanner.app.ui.calculators.goalbasedsip

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.GoalBasedSipInput
import com.financeplanner.app.domain.model.GoalBasedSipResult
import com.financeplanner.app.domain.usecase.CalculateGoalBasedSipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface GoalBasedSipValidationError {
    data object InvalidInput : GoalBasedSipValidationError
    data class InvalidValue(val rawMessage: String?) : GoalBasedSipValidationError
}

data class GoalBasedSipUiState(
    val targetAmount: String = "",
    val durationYears: String = "10",
    val expectedReturnPercent: String = "12",
    val inflationPercent: String = "",
    val result: GoalBasedSipResult? = null,
    val error: GoalBasedSipValidationError? = null
)

@HiltViewModel
class GoalBasedSipViewModel @Inject constructor(
    private val calculateGoalBasedSip: CalculateGoalBasedSipUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalBasedSipUiState())
    val uiState: StateFlow<GoalBasedSipUiState> = _uiState.asStateFlow()

    fun onTargetAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(targetAmount = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val targetAmount = state.targetAmount.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (targetAmount == null || duration == null || expectedReturn == null) {
            _uiState.value = state.copy(error = GoalBasedSipValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateGoalBasedSip(
                GoalBasedSipInput(targetAmount, duration, expectedReturn, inflation)
            )
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = GoalBasedSipValidationError.InvalidValue(e.message), result = null)
        }
    }
}
