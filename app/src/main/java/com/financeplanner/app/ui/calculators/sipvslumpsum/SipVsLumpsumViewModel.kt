package com.financeplanner.app.ui.calculators.sipvslumpsum

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.LumpsumInput
import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipVsLumpsumInput
import com.financeplanner.app.domain.model.SipVsLumpsumResult
import com.financeplanner.app.domain.usecase.CalculateSipVsLumpsumUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val durationYears: String = "10",
    val result: SipVsLumpsumResult? = null,
    val error: SipVsLumpsumValidationError? = null
)

@HiltViewModel
class SipVsLumpsumViewModel @Inject constructor(
    private val calculateSipVsLumpsum: CalculateSipVsLumpsumUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipVsLumpsumUiState())
    val uiState: StateFlow<SipVsLumpsumUiState> = _uiState.asStateFlow()

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

    fun calculate() {
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
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = SipVsLumpsumValidationError.InvalidValue(e.message), result = null)
        }
    }
}
