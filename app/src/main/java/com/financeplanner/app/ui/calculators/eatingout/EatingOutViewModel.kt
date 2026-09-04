package com.financeplanner.app.ui.calculators.eatingout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.EatingOutInput
import com.financeplanner.app.domain.model.EatingOutResult
import com.financeplanner.app.domain.usecase.CalculateEatingOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EatingOutValidationError {
    data object InvalidInput : EatingOutValidationError
    data class InvalidValue(val rawMessage: String?) : EatingOutValidationError
}

data class EatingOutUiState(
    val eatingOutMonthlySpend: String = "",
    val homeCookingMonthlySpend: String = "",
    val years: String = "",
    val expectedReturnPercent: String = "",
    val result: EatingOutResult? = null,
    val error: EatingOutValidationError? = null
)

@HiltViewModel
class EatingOutViewModel @Inject constructor(
    private val calculateEatingOut: CalculateEatingOutUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(EatingOutUiState())
    val uiState: StateFlow<EatingOutUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString()
            )
        }
    }

    fun onEatingOutSpendChange(value: String) {
        _uiState.value = _uiState.value.copy(eatingOutMonthlySpend = value, error = null)
    }
    fun onHomeCookingSpendChange(value: String) {
        _uiState.value = _uiState.value.copy(homeCookingMonthlySpend = value, error = null)
    }
    fun onYearsChange(value: String) {
        _uiState.value = _uiState.value.copy(years = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        val state = _uiState.value
        val eatingOutSpend = state.eatingOutMonthlySpend.toDoubleOrNull()
        val homeCookingSpend = state.homeCookingMonthlySpend.toDoubleOrNull()
        val years = state.years.toIntOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()

        if (eatingOutSpend == null || homeCookingSpend == null || years == null || expectedReturn == null) {
            _uiState.value = state.copy(error = EatingOutValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateEatingOut(
                EatingOutInput(
                    eatingOutMonthlySpend = eatingOutSpend,
                    homeCookingMonthlySpend = homeCookingSpend,
                    years = years,
                    expectedReturnPercent = expectedReturn
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = EatingOutValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "eating_out"
    }
}
