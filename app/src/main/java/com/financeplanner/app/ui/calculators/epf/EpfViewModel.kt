package com.financeplanner.app.ui.calculators.epf

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.EpfInput
import com.financeplanner.app.domain.model.EpfResult
import com.financeplanner.app.domain.usecase.CalculateEpfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface EpfValidationError {
    data object InvalidInput : EpfValidationError
    data class InvalidValue(val rawMessage: String?) : EpfValidationError
}

data class EpfUiState(
    val basicMonthlySalary: String = "",
    val employeeContributionPercent: String = "12",
    val employerContributionPercent: String = "12",
    val durationYears: String = "20",
    val expectedReturnPercent: String = "8.25",
    val result: EpfResult? = null,
    val error: EpfValidationError? = null
)

@HiltViewModel
class EpfViewModel @Inject constructor(
    private val calculateEpf: CalculateEpfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpfUiState())
    val uiState: StateFlow<EpfUiState> = _uiState.asStateFlow()

    fun onSalaryChange(value: String) {
        _uiState.value = _uiState.value.copy(basicMonthlySalary = value, error = null)
    }
    fun onEmployeeContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(employeeContributionPercent = value, error = null)
    }
    fun onEmployerContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(employerContributionPercent = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val salary = state.basicMonthlySalary.toDoubleOrNull()
        val employeePct = state.employeeContributionPercent.toDoubleOrNull()
        val employerPct = state.employerContributionPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()

        if (salary == null || employeePct == null || employerPct == null || duration == null || returnPct == null) {
            _uiState.value = state.copy(error = EpfValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateEpf(
                EpfInput(salary, employeePct, employerPct, duration, returnPct)
            )
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = EpfValidationError.InvalidValue(e.message), result = null)
        }
    }
}
