package com.financeplanner.app.ui.calculators.ssy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.SsyInput
import com.financeplanner.app.domain.model.SsyResult
import com.financeplanner.app.domain.usecase.CalculateSsyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SsyValidationError {
    data object InvalidInput : SsyValidationError
    data class InvalidValue(val rawMessage: String?) : SsyValidationError
}

data class SsyUiState(
    val girlAgeAtOpening: String = "",
    val yearlyDeposit: String = "",
    val interestRatePercent: String = "8.2",
    val inflationPercent: String = "",
    val result: SsyResult? = null,
    val error: SsyValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class SsyViewModel @Inject constructor(
    private val calculateSsy: CalculateSsyUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SsyUiState())
    val uiState: StateFlow<SsyUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(inflationPercent = prefs.defaultInflationPercent.toString())
        }
    }

    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(girlAgeAtOpening = value, error = null)
    }
    fun onDepositChange(value: String) {
        _uiState.value = _uiState.value.copy(yearlyDeposit = value, error = null)
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
        val age = state.girlAgeAtOpening.toIntOrNull()
        val deposit = state.yearlyDeposit.toDoubleOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (age == null || deposit == null || rate == null) {
            _uiState.value = state.copy(error = SsyValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateSsy(SsyInput(age, deposit, rate, inflationPercent = inflation))
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SsyValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "ssy"
    }
}
