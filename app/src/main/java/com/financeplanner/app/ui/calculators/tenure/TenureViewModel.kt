package com.financeplanner.app.ui.calculators.tenure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.TenureInput
import com.financeplanner.app.domain.model.TenureResult
import com.financeplanner.app.domain.usecase.CalculateTenureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TenureValidationError {
    data object InvalidInput : TenureValidationError
    data class InvalidValue(val rawMessage: String?) : TenureValidationError
}

data class TenureUiState(
    val monthlyAmount: String = "",
    val expectedReturnPercent: String = "12",
    val targetAmount: String = "",
    val result: TenureResult? = null,
    val error: TenureValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class TenureViewModel @Inject constructor(
    private val calculateTenure: CalculateTenureUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenureUiState())
    val uiState: StateFlow<TenureUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString())
        }
    }

    fun onMonthlyAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyAmount = value, error = null)
    }
    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onTargetAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(targetAmount = value, error = null)
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
        val monthlyAmount = state.monthlyAmount.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val targetAmount = state.targetAmount.toDoubleOrNull()

        if (monthlyAmount == null || expectedReturn == null || targetAmount == null) {
            _uiState.value = state.copy(error = TenureValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateTenure(
                TenureInput(monthlyAmount, expectedReturn, targetAmount)
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = TenureValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "tenure"
    }
}
