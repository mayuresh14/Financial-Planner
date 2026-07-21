package com.financeplanner.app.ui.calculators.goalbasedsip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.GoalBasedSipInput
import com.financeplanner.app.domain.model.GoalBasedSipResult
import com.financeplanner.app.domain.model.GoalType
import com.financeplanner.app.domain.usecase.CalculateGoalBasedSipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GoalBasedSipValidationError {
    data object InvalidInput : GoalBasedSipValidationError
    data class InvalidValue(val rawMessage: String?) : GoalBasedSipValidationError
}

data class GoalBasedSipUiState(
    val goalName: String = "",
    val goalType: GoalType = GoalType.RETIREMENT,
    val targetAmount: String = "",
    val durationYears: String = "",
    val expectedReturnPercent: String = "",
    val inflationPercent: String = "",
    val result: GoalBasedSipResult? = null,
    val error: GoalBasedSipValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

/**
 * Also covers what was previously a separate "Goal Planning" screen (named
 * goal + type + a gated "Save & Track" action) — that screen duplicated this
 * calculator's exact math, so it was merged in here instead of kept as a
 * second entry point. Return/inflation fields prefill from the user's saved
 * defaults (see AppPreferencesDataStore) rather than starting blank/hardcoded.
 */
@HiltViewModel
class GoalBasedSipViewModel @Inject constructor(
    private val calculateGoalBasedSip: CalculateGoalBasedSipUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalBasedSipUiState())
    val uiState: StateFlow<GoalBasedSipUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString(),
                inflationPercent = prefs.defaultInflationPercent.toString()
            )
        }
    }

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
    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
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
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = GoalBasedSipValidationError.InvalidValue(e.message), result = null)
        }
    }

    fun onSaveGoalClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        _uiState.value = _uiState.value.copy(showComingSoonSheet = true)
    }

    fun onComingSoonDismissed() {
        _uiState.value = _uiState.value.copy(showComingSoonSheet = false)
    }

    private companion object {
        const val CALCULATOR_NAME = "goal_based_sip"
    }
}
