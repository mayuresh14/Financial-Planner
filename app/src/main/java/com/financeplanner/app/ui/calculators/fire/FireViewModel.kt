package com.financeplanner.app.ui.calculators.fire

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.FireInput
import com.financeplanner.app.domain.model.FireResult
import com.financeplanner.app.domain.model.FireVariant
import com.financeplanner.app.domain.usecase.CalculateFireUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface FireValidationError {
    data object InvalidInput : FireValidationError
    data class InvalidValue(val rawMessage: String?) : FireValidationError
}

data class FireUiState(
    val variant: FireVariant = FireVariant.TRADITIONAL,
    val currentAge: String = "30",
    val retirementAge: String = "50",
    val currentAnnualExpenses: String = "",
    val inflationPercent: String = "6",
    val preRetirementReturnPercent: String = "12",
    val existingCorpus: String = "0",
    val result: FireResult? = null,
    val error: FireValidationError? = null
)

@HiltViewModel
class FireViewModel @Inject constructor(
    private val calculateFire: CalculateFireUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FireUiState())
    val uiState: StateFlow<FireUiState> = _uiState.asStateFlow()

    fun onVariantChange(variant: FireVariant) {
        _uiState.value = _uiState.value.copy(variant = variant, error = null, result = null)
    }
    fun onCurrentAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(currentAge = value, error = null)
    }
    fun onRetirementAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(retirementAge = value, error = null)
    }
    fun onExpensesChange(value: String) {
        _uiState.value = _uiState.value.copy(currentAnnualExpenses = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(preRetirementReturnPercent = value, error = null)
    }
    fun onExistingCorpusChange(value: String) {
        _uiState.value = _uiState.value.copy(existingCorpus = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val currentAge = state.currentAge.toIntOrNull()
        val retirementAge = state.retirementAge.toIntOrNull()
        val expenses = state.currentAnnualExpenses.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val returnPct = state.preRetirementReturnPercent.toDoubleOrNull()
        val existingCorpus = state.existingCorpus.toDoubleOrNull() ?: 0.0

        if (currentAge == null || retirementAge == null || expenses == null || inflation == null || returnPct == null) {
            _uiState.value = state.copy(error = FireValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateFire(
                FireInput(state.variant, currentAge, retirementAge, expenses, inflation, returnPct, existingCorpus)
            )
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = FireValidationError.InvalidValue(e.message), result = null)
        }
    }
}
