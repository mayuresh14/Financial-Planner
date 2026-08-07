package com.financeplanner.app.ui.calculators.sip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.SipFrequency
import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipResult
import com.financeplanner.app.domain.usecase.CalculateSipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Validation errors as a sealed type rather than a raw string — the
 * ViewModel has no Context/string-resource access, so the UI layer resolves
 * these to a localized message via stringResource(). Keeps the ViewModel
 * localization-agnostic and unit-testable without Robolectric.
 */
sealed interface SipValidationError {
    data object InvalidInput : SipValidationError
    data class InvalidValue(val rawMessage: String?) : SipValidationError
}

/** Step-up is either a percentage or a flat rupee amount added yearly — never both at once. */
enum class StepUpMode { PERCENTAGE, FIXED_AMOUNT }

/**
 * Form + result state for the SIP calculator screen. All fields are kept as
 * raw strings for TextField binding; parsing/validation happens on Calculate.
 * This is the state shape every other calculator's ViewModel will mirror.
 */
data class SipUiState(
    val monthlyAmount: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "",
    val frequency: SipFrequency = SipFrequency.MONTHLY,
    val showAdvancedOptions: Boolean = false,
    val stepUpMode: StepUpMode = StepUpMode.PERCENTAGE,
    val stepUpPercent: String = "",
    val stepUpFixedAmount: String = "",
    val expenseRatioPercent: String = "",
    val inflationPercent: String = "",
    val initialLumpsum: String = "",
    val result: SipResult? = null,
    val error: SipValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class SipViewModel @Inject constructor(
    private val calculateSip: CalculateSipUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipUiState())
    val uiState: StateFlow<SipUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString(),
                inflationPercent = prefs.defaultInflationPercent.toString()
            )
        }
    }

    fun onMonthlyAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyAmount = value, error = null)
    }

    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }

    fun onFrequencyChange(frequency: SipFrequency) {
        _uiState.value = _uiState.value.copy(frequency = frequency, error = null)
    }

    fun onToggleAdvancedOptions() {
        _uiState.value = _uiState.value.copy(showAdvancedOptions = !_uiState.value.showAdvancedOptions)
    }

    fun onStepUpModeChange(mode: StepUpMode) {
        _uiState.value = _uiState.value.copy(stepUpMode = mode, error = null)
    }

    fun onStepUpChange(value: String) {
        _uiState.value = _uiState.value.copy(stepUpPercent = value, error = null)
    }

    fun onStepUpFixedAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(stepUpFixedAmount = value, error = null)
    }

    fun onExpenseRatioChange(value: String) {
        _uiState.value = _uiState.value.copy(expenseRatioPercent = value, error = null)
    }

    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun onInitialLumpsumChange(value: String) {
        _uiState.value = _uiState.value.copy(initialLumpsum = value, error = null)
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
        val duration = state.durationYears.toIntOrNull()
        val stepUpPercent = if (state.stepUpMode == StepUpMode.PERCENTAGE) state.stepUpPercent.toDoubleOrNull() else null
        val stepUpFixedAmount = if (state.stepUpMode == StepUpMode.FIXED_AMOUNT) state.stepUpFixedAmount.toDoubleOrNull() else null
        val expenseRatio = state.expenseRatioPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val initialLumpsum = state.initialLumpsum.toDoubleOrNull()

        if (monthlyAmount == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = SipValidationError.InvalidInput, result = null)
            return
        }

        try {
            val input = SipInput(
                contributionAmount = monthlyAmount,
                expectedReturnPercent = expectedReturn,
                durationYears = duration,
                frequency = state.frequency,
                stepUpPercent = stepUpPercent,
                stepUpFixedAmount = stepUpFixedAmount,
                expenseRatioPercent = expenseRatio,
                inflationPercent = inflation,
                initialLumpsum = initialLumpsum
            )
            val result = calculateSip(input)
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SipValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "sip"
    }
}
