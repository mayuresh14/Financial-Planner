package com.financeplanner.app.ui.calculators.ppf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.PpfInput
import com.financeplanner.app.domain.model.PpfResult
import com.financeplanner.app.domain.usecase.CalculatePpfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PpfValidationError {
    data object InvalidInput : PpfValidationError
    data class InvalidValue(val rawMessage: String?) : PpfValidationError
}

data class PpfUiState(
    val yearlyContribution: String = "",
    val durationYears: String = "",
    val interestRatePercent: String = "7.1",
    val inflationPercent: String = "",
    val result: PpfResult? = null,
    val error: PpfValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class PpfViewModel @Inject constructor(
    private val calculatePpf: CalculatePpfUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(PpfUiState())
    val uiState: StateFlow<PpfUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(inflationPercent = prefs.defaultInflationPercent.toString())
        }
    }

    fun onYearlyContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(yearlyContribution = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(interestRatePercent = value, error = null)
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
        val yearlyContribution = state.yearlyContribution.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (yearlyContribution == null || duration == null || rate == null) {
            _uiState.value = state.copy(error = PpfValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculatePpf(PpfInput(yearlyContribution, duration, rate, inflation))
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = PpfValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "ppf"
    }
}
