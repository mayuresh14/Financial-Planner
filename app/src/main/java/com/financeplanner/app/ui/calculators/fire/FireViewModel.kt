package com.financeplanner.app.ui.calculators.fire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.FireAgeInput
import com.financeplanner.app.domain.model.FireAgeResult
import com.financeplanner.app.domain.model.FireCalculationMode
import com.financeplanner.app.domain.model.FireInput
import com.financeplanner.app.domain.model.FireResult
import com.financeplanner.app.domain.model.FireVariant
import com.financeplanner.app.domain.usecase.CalculateFireAgeUseCase
import com.financeplanner.app.domain.usecase.CalculateFireUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FireValidationError {
    data object InvalidInput : FireValidationError
    data class InvalidValue(val rawMessage: String?) : FireValidationError
}

data class FireUiState(
    val mode: FireCalculationMode = FireCalculationMode.FIND_SIP,
    val variant: FireVariant = FireVariant.TRADITIONAL,
    val currentAge: String = "",
    val retirementAge: String = "",
    val currentAnnualExpenses: String = "",
    val inflationPercent: String = "6",
    val preRetirementReturnPercent: String = "12",
    val existingCorpus: String = "",
    val monthlySip: String = "",
    val result: FireResult? = null,
    val ageResult: FireAgeResult? = null,
    val error: FireValidationError? = null
)

@HiltViewModel
class FireViewModel @Inject constructor(
    private val calculateFire: CalculateFireUseCase,
    private val calculateFireAge: CalculateFireAgeUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(FireUiState())
    val uiState: StateFlow<FireUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                inflationPercent = prefs.defaultInflationPercent.toString(),
                preRetirementReturnPercent = prefs.defaultExpectedReturnPercent.toString()
            )
        }
    }

    fun onModeChange(mode: FireCalculationMode) {
        _uiState.value = _uiState.value.copy(mode = mode, error = null, result = null, ageResult = null)
    }
    fun onVariantChange(variant: FireVariant) {
        _uiState.value = _uiState.value.copy(variant = variant, error = null, result = null, ageResult = null)
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
    fun onMonthlySipChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlySip = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null, ageResult = null)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        val state = _uiState.value
        val currentAge = state.currentAge.toIntOrNull()
        val expenses = state.currentAnnualExpenses.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val returnPct = state.preRetirementReturnPercent.toDoubleOrNull()
        val existingCorpus = state.existingCorpus.toDoubleOrNull() ?: 0.0

        if (state.mode == FireCalculationMode.FIND_SIP) {
            val retirementAge = state.retirementAge.toIntOrNull()
            if (currentAge == null || retirementAge == null || expenses == null || inflation == null || returnPct == null) {
                _uiState.value = state.copy(error = FireValidationError.InvalidInput, result = null, ageResult = null)
                return
            }
            try {
                val result = calculateFire(
                    FireInput(state.variant, currentAge, retirementAge, expenses, inflation, returnPct, existingCorpus)
                )
                _uiState.value = state.copy(result = result, ageResult = null, error = null)
                analytics.logResultViewed(CALCULATOR_NAME)
            } catch (e: IllegalArgumentException) {
                analytics.recordException(e, CALCULATOR_NAME)
                _uiState.value = state.copy(error = FireValidationError.InvalidValue(e.message), result = null, ageResult = null)
            }
        } else {
            val monthlySip = state.monthlySip.toDoubleOrNull()
            if (currentAge == null || expenses == null || inflation == null || returnPct == null || monthlySip == null) {
                _uiState.value = state.copy(error = FireValidationError.InvalidInput, result = null, ageResult = null)
                return
            }
            try {
                val result = calculateFireAge(
                    FireAgeInput(state.variant, currentAge, expenses, inflation, returnPct, monthlySip, existingCorpus)
                )
                _uiState.value = state.copy(ageResult = result, result = null, error = null)
                analytics.logResultViewed(CALCULATOR_NAME)
            } catch (e: IllegalArgumentException) {
                analytics.recordException(e, CALCULATOR_NAME)
                _uiState.value = state.copy(error = FireValidationError.InvalidValue(e.message), result = null, ageResult = null)
            }
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "fire"
    }
}
