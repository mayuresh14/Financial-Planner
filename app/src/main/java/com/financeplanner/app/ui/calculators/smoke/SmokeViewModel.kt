package com.financeplanner.app.ui.calculators.smoke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.SmokeInput
import com.financeplanner.app.domain.model.SmokeResult
import com.financeplanner.app.domain.usecase.CalculateSmokeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SmokeValidationError {
    data object InvalidInput : SmokeValidationError
    data class InvalidValue(val rawMessage: String?) : SmokeValidationError
}

data class SmokeUiState(
    val cigarettesPerDay: String = "",
    val costPerCigarette: String = "",
    val years: String = "",
    val expectedReturnPercent: String = "",
    val inflationPercent: String = "",
    val result: SmokeResult? = null,
    val error: SmokeValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class SmokeViewModel @Inject constructor(
    private val calculateSmoke: CalculateSmokeUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmokeUiState())
    val uiState: StateFlow<SmokeUiState> = _uiState.asStateFlow()

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

    fun onCigarettesPerDayChange(value: String) {
        _uiState.value = _uiState.value.copy(cigarettesPerDay = value, error = null)
    }
    fun onCostPerCigaretteChange(value: String) {
        _uiState.value = _uiState.value.copy(costPerCigarette = value, error = null)
    }
    fun onYearsChange(value: String) {
        _uiState.value = _uiState.value.copy(years = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
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
        val cigarettesPerDay = state.cigarettesPerDay.toIntOrNull()
        val costPerCigarette = state.costPerCigarette.toDoubleOrNull()
        val years = state.years.toIntOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (cigarettesPerDay == null || costPerCigarette == null || years == null || expectedReturn == null) {
            _uiState.value = state.copy(error = SmokeValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateSmoke(
                SmokeInput(
                    cigarettesPerDay = cigarettesPerDay,
                    costPerCigarette = costPerCigarette,
                    years = years,
                    expectedReturnPercent = expectedReturn,
                    inflationPercent = inflation
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SmokeValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "smoke"
    }
}
