package com.financeplanner.app.ui.calculators.fd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.FdInput
import com.financeplanner.app.domain.model.FdResult
import com.financeplanner.app.domain.usecase.CalculateFdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FdValidationError {
    data object InvalidInput : FdValidationError
    data class InvalidValue(val rawMessage: String?) : FdValidationError
}

data class FdUiState(
    val principal: String = "",
    val annualRatePercent: String = "",
    val tenureMonths: String = "",
    val inflationPercent: String = "",
    val result: FdResult? = null,
    val error: FdValidationError? = null,
    val showComingSoonSheet: Boolean = false
)

@HiltViewModel
class FdViewModel @Inject constructor(
    private val calculateFd: CalculateFdUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(FdUiState())
    val uiState: StateFlow<FdUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(inflationPercent = prefs.defaultInflationPercent.toString())
        }
    }

    fun onPrincipalChange(value: String) {
        _uiState.value = _uiState.value.copy(principal = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(annualRatePercent = value, error = null)
    }
    fun onTenureChange(value: String) {
        _uiState.value = _uiState.value.copy(tenureMonths = value, error = null)
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
        val rate = state.annualRatePercent.toDoubleOrNull()
        val tenure = state.tenureMonths.toIntOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (principal == null || rate == null || tenure == null) {
            _uiState.value = state.copy(error = FdValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateFd(
                FdInput(
                    principal = principal,
                    annualRatePercent = rate,
                    tenureMonths = tenure,
                    inflationPercent = inflation
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = FdValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "fd"
    }
}
