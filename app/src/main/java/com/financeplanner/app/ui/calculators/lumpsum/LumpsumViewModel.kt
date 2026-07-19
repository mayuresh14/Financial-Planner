package com.financeplanner.app.ui.calculators.lumpsum

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.LumpsumInput
import com.financeplanner.app.domain.model.LumpsumResult
import com.financeplanner.app.domain.usecase.CalculateLumpsumUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface LumpsumValidationError {
    data object InvalidInput : LumpsumValidationError
    data class InvalidValue(val rawMessage: String?) : LumpsumValidationError
}

data class LumpsumUiState(
    val principal: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "10",
    val inflationPercent: String = "",
    val result: LumpsumResult? = null,
    val error: LumpsumValidationError? = null
)

@HiltViewModel
class LumpsumViewModel @Inject constructor(
    private val calculateLumpsum: CalculateLumpsumUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LumpsumUiState())
    val uiState: StateFlow<LumpsumUiState> = _uiState.asStateFlow()

    fun onPrincipalChange(value: String) {
        _uiState.value = _uiState.value.copy(principal = value, error = null)
    }

    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }

    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val principal = state.principal.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (principal == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = LumpsumValidationError.InvalidInput, result = null)
            return
        }

        try {
            val input = LumpsumInput(
                principal = principal,
                expectedReturnPercent = expectedReturn,
                durationYears = duration,
                inflationPercent = inflation
            )
            val result = calculateLumpsum(input)
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = LumpsumValidationError.InvalidValue(e.message), result = null)
        }
    }
}
