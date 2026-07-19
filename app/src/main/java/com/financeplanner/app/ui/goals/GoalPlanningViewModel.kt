package com.financeplanner.app.ui.goals

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.GoalBasedSipInput
import com.financeplanner.app.domain.model.GoalPlanResult
import com.financeplanner.app.domain.model.GoalType
import com.financeplanner.app.domain.usecase.CalculateGoalBasedSipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface GoalPlanValidationError {
    data object InvalidInput : GoalPlanValidationError
    data class InvalidValue(val rawMessage: String?) : GoalPlanValidationError
}

data class GoalPlanningUiState(
    val goalName: String = "",
    val goalType: GoalType = GoalType.RETIREMENT,
    val targetAmount: String = "",
    val durationYears: String = "10",
    val expectedReturnPercent: String = "12",
    val inflationPercent: String = "",
    val result: GoalPlanResult? = null,
    val error: GoalPlanValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

/**
 * Reuses the Goal-based SIP math (same formula, same UseCase) but frames it
 * as a named goal with a type, per the requirements doc's Goal Planning
 * module. Calculating is guest-accessible; "Save & Track" is the one
 * gated action, routed through ComingSoonSheet since Phase 1 has no backend.
 */
@HiltViewModel
class GoalPlanningViewModel @Inject constructor(
    private val calculateGoalBasedSip: CalculateGoalBasedSipUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalPlanningUiState())
    val uiState: StateFlow<GoalPlanningUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(goalName = value, error = null)
    }
    fun onTypeChange(type: GoalType) {
        _uiState.value = _uiState.value.copy(goalType = type, error = null)
    }
    fun onTargetAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(targetAmount = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onReturnChange(value: String) {
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

        if (state.goalName.isBlank() || targetAmount == null || duration == null || expectedReturn == null) {
            _uiState.value = state.copy(error = GoalPlanValidationError.InvalidInput, result = null)
            return
        }
        try {
            val sipResult = calculateGoalBasedSip(
                GoalBasedSipInput(targetAmount, duration, expectedReturn, inflation)
            )
            _uiState.value = state.copy(
                result = GoalPlanResult(sipResult.requiredMonthlySip, sipResult.inflationAdjustedTarget),
                error = null
            )
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = GoalPlanValidationError.InvalidValue(e.message), result = null)
        }
    }

    fun onSaveGoalClicked() {
        _uiState.value = _uiState.value.copy(showComingSoonSheet = true)
    }

    fun onComingSoonDismissed() {
        _uiState.value = _uiState.value.copy(showComingSoonSheet = false)
    }
}
