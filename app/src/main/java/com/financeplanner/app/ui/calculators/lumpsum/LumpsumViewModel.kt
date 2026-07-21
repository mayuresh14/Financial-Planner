package com.financeplanner.app.ui.calculators.lumpsum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.LumpsumInput
import com.financeplanner.app.domain.model.LumpsumResult
import com.financeplanner.app.domain.usecase.CalculateLumpsumUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LumpsumValidationError {
    data object InvalidInput : LumpsumValidationError
    data class InvalidValue(val rawMessage: String?) : LumpsumValidationError
}

data class LumpsumUiState(
    val principal: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "",
    val inflationPercent: String = "",
    val result: LumpsumResult? = null,
    val error: LumpsumValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class LumpsumViewModel @Inject constructor(
    private val calculateLumpsum: CalculateLumpsumUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(LumpsumUiState())
    val uiState: StateFlow<LumpsumUiState> = _uiState.asStateFlow()

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

    fun onPrincipalChange(value: String) {
        _uiState.value = _uiState.value.copy(principal = value, error = null)
    }

    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
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
        val principal = state.principal.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (principal == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = LumpsumValidationError.InvalidInput, result = null)
            return
        }

        try {
            val input = LumpsumInput(
                principal = principal,
                expectedReturnPercent = expectedReturn,
                durationYears = duration,
                inflationPercent = inflation
            )
            val result = calculateLumpsum(input)
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = LumpsumValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "lumpsum"
    }
}
