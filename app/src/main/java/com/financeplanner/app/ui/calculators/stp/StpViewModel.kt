package com.financeplanner.app.ui.calculators.stp

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.StpInput
import com.financeplanner.app.domain.model.StpResult
import com.financeplanner.app.domain.usecase.CalculateStpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface StpValidationError {
    data object InvalidInput : StpValidationError
    data class InvalidValue(val rawMessage: String?) : StpValidationError
}

data class StpUiState(
    val sourceLumpsum: String = "",
    val monthlyTransferAmount: String = "",
    val durationMonths: String = "12",
    val sourceReturnPercent: String = "7",
    val targetReturnPercent: String = "12",
    val result: StpResult? = null,
    val error: StpValidationError? = null
)

@HiltViewModel
class StpViewModel @Inject constructor(
    private val calculateStp: CalculateStpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StpUiState())
    val uiState: StateFlow<StpUiState> = _uiState.asStateFlow()

    fun onSourceLumpsumChange(value: String) {
        _uiState.value = _uiState.value.copy(sourceLumpsum = value, error = null)
    }
    fun onTransferAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyTransferAmount = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationMonths = value, error = null)
    }
    fun onSourceReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(sourceReturnPercent = value, error = null)
    }
    fun onTargetReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(targetReturnPercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val sourceLumpsum = state.sourceLumpsum.toDoubleOrNull()
        val transferAmount = state.monthlyTransferAmount.toDoubleOrNull()
        val duration = state.durationMonths.toIntOrNull()
        val sourceReturn = state.sourceReturnPercent.toDoubleOrNull()
        val targetReturn = state.targetReturnPercent.toDoubleOrNull()

        if (sourceLumpsum == null || transferAmount == null || duration == null || sourceReturn == null || targetReturn == null) {
            _uiState.value = state.copy(error = StpValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateStp(
                StpInput(sourceLumpsum, transferAmount, duration, sourceReturn, targetReturn)
            )
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = StpValidationError.InvalidValue(e.message), result = null)
        }
    }
}
