package com.financeplanner.app.ui.calculators.gratuity

import androidx.lifecycle.ViewModel
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.domain.model.GratuityInput
import com.financeplanner.app.domain.model.GratuityResult
import com.financeplanner.app.domain.usecase.CalculateGratuityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface GratuityValidationError {
    data object InvalidInput : GratuityValidationError
    data class InvalidValue(val rawMessage: String?) : GratuityValidationError
}

data class GratuityUiState(
    val lastDrawnMonthlySalary: String = "",
    val yearsOfService: String = "",
    val result: GratuityResult? = null,
    val error: GratuityValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class GratuityViewModel @Inject constructor(
    private val calculateGratuity: CalculateGratuityUseCase,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(GratuityUiState())
    val uiState: StateFlow<GratuityUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
    }

    fun onSalaryChange(value: String) {
        _uiState.value = _uiState.value.copy(lastDrawnMonthlySalary = value, error = null)
    }
    fun onYearsOfServiceChange(value: String) {
        _uiState.value = _uiState.value.copy(yearsOfService = value, error = null)
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
        val salary = state.lastDrawnMonthlySalary.toDoubleOrNull()
        val years = state.yearsOfService.toIntOrNull()

        if (salary == null || years == null) {
            _uiState.value = state.copy(error = GratuityValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateGratuity(
                GratuityInput(
                    lastDrawnMonthlySalary = salary,
                    yearsOfService = years
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = GratuityValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "gratuity"
    }
}
