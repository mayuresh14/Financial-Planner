package com.financeplanner.app.ui.calculators.ppf

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.PpfInput
import com.financeplanner.app.domain.model.PpfResult
import com.financeplanner.app.domain.usecase.CalculatePpfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface PpfValidationError {
    data object InvalidInput : PpfValidationError
    data class InvalidValue(val rawMessage: String?) : PpfValidationError
}

data class PpfUiState(
    val yearlyContribution: String = "",
    val durationYears: String = "15",
    val interestRatePercent: String = "7.1",
    val result: PpfResult? = null,
    val error: PpfValidationError? = null
)

@HiltViewModel
class PpfViewModel @Inject constructor(
    private val calculatePpf: CalculatePpfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PpfUiState())
    val uiState: StateFlow<PpfUiState> = _uiState.asStateFlow()

    fun onYearlyContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(yearlyContribution = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(interestRatePercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val yearlyContribution = state.yearlyContribution.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()

        if (yearlyContribution == null || duration == null || rate == null) {
            _uiState.value = state.copy(error = PpfValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculatePpf(PpfInput(yearlyContribution, duration, rate))
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = PpfValidationError.InvalidValue(e.message), result = null)
        }
    }
}
