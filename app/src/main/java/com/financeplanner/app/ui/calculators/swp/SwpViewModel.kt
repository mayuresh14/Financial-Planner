package com.financeplanner.app.ui.calculators.swp

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.SwpInput
import com.financeplanner.app.domain.model.SwpResult
import com.financeplanner.app.domain.usecase.CalculateSwpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface SwpValidationError {
    data object InvalidInput : SwpValidationError
    data class InvalidValue(val rawMessage: String?) : SwpValidationError
}

data class SwpUiState(
    val initialCorpus: String = "",
    val monthlyWithdrawal: String = "",
    val expectedReturnPercent: String = "7",
    val simulateYears: String = "30",
    val result: SwpResult? = null,
    val error: SwpValidationError? = null
)

@HiltViewModel
class SwpViewModel @Inject constructor(
    private val calculateSwp: CalculateSwpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwpUiState())
    val uiState: StateFlow<SwpUiState> = _uiState.asStateFlow()

    fun onCorpusChange(value: String) {
        _uiState.value = _uiState.value.copy(initialCorpus = value, error = null)
    }
    fun onWithdrawalChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyWithdrawal = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onSimulateYearsChange(value: String) {
        _uiState.value = _uiState.value.copy(simulateYears = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val corpus = state.initialCorpus.toDoubleOrNull()
        val withdrawal = state.monthlyWithdrawal.toDoubleOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val years = state.simulateYears.toIntOrNull()

        if (corpus == null || withdrawal == null || returnPct == null || years == null) {
            _uiState.value = state.copy(error = SwpValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateSwp(SwpInput(corpus, withdrawal, returnPct, years))
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = SwpValidationError.InvalidValue(e.message), result = null)
        }
    }
}
