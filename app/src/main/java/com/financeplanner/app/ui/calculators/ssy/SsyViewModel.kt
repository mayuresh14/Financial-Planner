package com.financeplanner.app.ui.calculators.ssy

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.SsyInput
import com.financeplanner.app.domain.model.SsyResult
import com.financeplanner.app.domain.usecase.CalculateSsyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface SsyValidationError {
    data object InvalidInput : SsyValidationError
    data class InvalidValue(val rawMessage: String?) : SsyValidationError
}

data class SsyUiState(
    val girlAgeAtOpening: String = "",
    val yearlyDeposit: String = "",
    val interestRatePercent: String = "8.2",
    val result: SsyResult? = null,
    val error: SsyValidationError? = null
)

@HiltViewModel
class SsyViewModel @Inject constructor(
    private val calculateSsy: CalculateSsyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SsyUiState())
    val uiState: StateFlow<SsyUiState> = _uiState.asStateFlow()

    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(girlAgeAtOpening = value, error = null)
    }
    fun onDepositChange(value: String) {
        _uiState.value = _uiState.value.copy(yearlyDeposit = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(interestRatePercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val age = state.girlAgeAtOpening.toIntOrNull()
        val deposit = state.yearlyDeposit.toDoubleOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()

        if (age == null || deposit == null || rate == null) {
            _uiState.value = state.copy(error = SsyValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateSsy(SsyInput(age, deposit, rate))
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = SsyValidationError.InvalidValue(e.message), result = null)
        }
    }
}
