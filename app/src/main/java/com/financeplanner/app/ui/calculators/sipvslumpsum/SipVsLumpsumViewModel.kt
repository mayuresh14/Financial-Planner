package com.financeplanner.app.ui.calculators.sipvslumpsum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.LumpsumInput
import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipVsLumpsumInput
import com.financeplanner.app.domain.model.SipVsLumpsumResult
import com.financeplanner.app.domain.usecase.CalculateSipVsLumpsumUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SipVsLumpsumValidationError {
    data object InvalidInput : SipVsLumpsumValidationError
    data class InvalidValue(val rawMessage: String?) : SipVsLumpsumValidationError
}

// Both scenarios share one duration/return input for a fair side-by-side
// comparison; SIP monthly amount and Lumpsum principal are entered separately.
data class SipVsLumpsumUiState(
    val monthlyAmount: String = "",
    val lumpsumAmount: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "",
    val result: SipVsLumpsumResult? = null,
    val error: SipVsLumpsumValidationError? = null
)

@HiltViewModel
class SipVsLumpsumViewModel @Inject constructor(
    private val calculateSipVsLumpsum: CalculateSipVsLumpsumUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipVsLumpsumUiState())
    val uiState: StateFlow<SipVsLumpsumUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString())
        }
    }

    fun onMonthlyAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyAmount = value, error = null)
    }
    fun onLumpsumAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(lumpsumAmount = value, error = null)
    }
    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        val state = _uiState.value
        val monthlyAmount = state.monthlyAmount.toDoubleOrNull()
        val lumpsumAmount = state.lumpsumAmount.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()

        if (monthlyAmount == null || lumpsumAmount == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = SipVsLumpsumValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateSipVsLumpsum(
                SipVsLumpsumInput(
                    sipInput = SipInput(monthlyAmount, expectedReturn, duration),
                    lumpsumInput = LumpsumInput(lumpsumAmount, expectedReturn, duration)
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SipVsLumpsumValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "sip_vs_lumpsum"
    }
}
