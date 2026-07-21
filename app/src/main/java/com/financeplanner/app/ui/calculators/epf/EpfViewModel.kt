package com.financeplanner.app.ui.calculators.epf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.EpfInput
import com.financeplanner.app.domain.model.EpfResult
import com.financeplanner.app.domain.usecase.CalculateEpfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EpfValidationError {
    data object InvalidInput : EpfValidationError
    data class InvalidValue(val rawMessage: String?) : EpfValidationError
}

data class EpfUiState(
    val basicMonthlySalary: String = "",
    val employeeContributionPercent: String = "",
    val employerContributionPercent: String = "",
    val durationYears: String = "",
    val expectedReturnPercent: String = "8.25",
    val inflationPercent: String = "",
    val result: EpfResult? = null,
    val error: EpfValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class EpfViewModel @Inject constructor(
    private val calculateEpf: CalculateEpfUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpfUiState())
    val uiState: StateFlow<EpfUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(inflationPercent = prefs.defaultInflationPercent.toString())
        }
    }

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
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
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
        val salary = state.basicMonthlySalary.toDoubleOrNull()
        val employeePct = state.employeeContributionPercent.toDoubleOrNull()
        val employerPct = state.employerContributionPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (salary == null || employeePct == null || employerPct == null || duration == null || returnPct == null) {
            _uiState.value = state.copy(error = EpfValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateEpf(
                EpfInput(salary, employeePct, employerPct, duration, returnPct, inflationPercent = inflation)
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = EpfValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "epf"
    }
}
