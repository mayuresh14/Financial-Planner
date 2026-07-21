package com.financeplanner.app.ui.calculators.swp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.SwpInput
import com.financeplanner.app.domain.model.SwpResult
import com.financeplanner.app.domain.usecase.CalculateSwpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SwpValidationError {
    data object InvalidInput : SwpValidationError
    data class InvalidValue(val rawMessage: String?) : SwpValidationError
}

data class SwpUiState(
    val initialCorpus: String = "",
    val monthlyWithdrawal: String = "",
    val expectedReturnPercent: String = "7",
    val simulateYears: String = "",
    val result: SwpResult? = null,
    val error: SwpValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class SwpViewModel @Inject constructor(
    private val calculateSwp: CalculateSwpUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwpUiState())
    val uiState: StateFlow<SwpUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString())
        }
    }

    fun onCorpusChange(value: String) {
        _uiState.value = _uiState.value.copy(initialCorpus = value, error = null)
    }
    fun onWithdrawalChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyWithdrawal = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onSimulateYearsChange(value: String) {
        _uiState.value = _uiState.value.copy(simulateYears = value, error = null)
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
        val corpus = state.initialCorpus.toDoubleOrNull()
        val withdrawal = state.monthlyWithdrawal.toDoubleOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val years = state.simulateYears.toIntOrNull()

        if (corpus == null || withdrawal == null || returnPct == null || years == null) {
            _uiState.value = state.copy(error = SwpValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateSwp(SwpInput(corpus, withdrawal, returnPct, years))
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SwpValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "swp"
    }
}
