package com.financeplanner.app.ui.calculators.nps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.NpsInput
import com.financeplanner.app.domain.model.NpsResult
import com.financeplanner.app.domain.usecase.CalculateNpsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NpsValidationError {
    data object InvalidInput : NpsValidationError
    data class InvalidValue(val rawMessage: String?) : NpsValidationError
}

data class NpsUiState(
    val monthlyContribution: String = "",
    val currentAge: String = "",
    val expectedReturnPercent: String = "10",
    val annuityPercent: String = "",
    val inflationPercent: String = "",
    val result: NpsResult? = null,
    val error: NpsValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class NpsViewModel @Inject constructor(
    private val calculateNps: CalculateNpsUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(NpsUiState())
    val uiState: StateFlow<NpsUiState> = _uiState.asStateFlow()

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

    fun onContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyContribution = value, error = null)
    }
    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(currentAge = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onAnnuityChange(value: String) {
        _uiState.value = _uiState.value.copy(annuityPercent = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
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
        val contribution = state.monthlyContribution.toDoubleOrNull()
        val age = state.currentAge.toIntOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val annuityPct = state.annuityPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (contribution == null || age == null || returnPct == null || annuityPct == null) {
            _uiState.value = state.copy(error = NpsValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateNps(NpsInput(contribution, age, returnPct, annuityPct, inflationPercent = inflation))
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = NpsValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "nps"
    }
}
