package com.financeplanner.app.ui.calculators.sip

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipResult
import com.financeplanner.app.domain.usecase.CalculateSipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Validation errors as a sealed type rather than a raw string — the
 * ViewModel has no Context/string-resource access, so the UI layer resolves
 * these to a localized message via stringResource(). Keeps the ViewModel
 * localization-agnostic and unit-testable without Robolectric.
 */
sealed interface SipValidationError {
    data object InvalidInput : SipValidationError
    data class InvalidValue(val rawMessage: String?) : SipValidationError
}

/**
 * Form + result state for the SIP calculator screen. All fields are kept as
 * raw strings for TextField binding; parsing/validation happens on Calculate.
 * This is the state shape every other calculator's ViewModel will mirror.
 */
data class SipUiState(
    val monthlyAmount: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "10",
    val stepUpPercent: String = "",
    val inflationPercent: String = "",
    val result: SipResult? = null,
    val error: SipValidationError? = null
)

@HiltViewModel
class SipViewModel @Inject constructor(
    private val calculateSip: CalculateSipUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipUiState())
    val uiState: StateFlow<SipUiState> = _uiState.asStateFlow()

    fun onMonthlyAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyAmount = value, error = null)
    }

    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }

    fun onStepUpChange(value: String) {
        _uiState.value = _uiState.value.copy(stepUpPercent = value, error = null)
    }

    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val monthlyAmount = state.monthlyAmount.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val stepUp = state.stepUpPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (monthlyAmount == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = SipValidationError.InvalidInput, result = null)
            return
        }

        try {
            val input = SipInput(
                monthlyAmount = monthlyAmount,
                expectedReturnPercent = expectedReturn,
                durationYears = duration,
                stepUpPercent = stepUp,
                inflationPercent = inflation
            )
            val result = calculateSip(input)
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = SipValidationError.InvalidValue(e.message), result = null)
        }
    }
}
