package com.financeplanner.app.ui.calculators.emi

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.EmiInput
import com.financeplanner.app.domain.model.EmiResult
import com.financeplanner.app.domain.model.PrepaymentStrategy
import com.financeplanner.app.domain.usecase.CalculateEmiUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface EmiValidationError {
    data object InvalidInput : EmiValidationError
    data class InvalidValue(val rawMessage: String?) : EmiValidationError
}

data class EmiUiState(
    val loanAmount: String = "",
    val interestRatePercent: String = "9",
    val tenureMonths: String = "240",
    val enablePrepayment: Boolean = false,
    val prepaymentAmount: String = "",
    val prepaymentAfterMonth: String = "",
    val prepaymentStrategy: PrepaymentStrategy = PrepaymentStrategy.REDUCE_TENURE,
    val result: EmiResult? = null,
    val error: EmiValidationError? = null
)

@HiltViewModel
class EmiViewModel @Inject constructor(
    private val calculateEmi: CalculateEmiUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmiUiState())
    val uiState: StateFlow<EmiUiState> = _uiState.asStateFlow()

    fun onLoanAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(loanAmount = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(interestRatePercent = value, error = null)
    }
    fun onTenureChange(value: String) {
        _uiState.value = _uiState.value.copy(tenureMonths = value, error = null)
    }
    fun onTogglePrepayment(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enablePrepayment = enabled, error = null)
    }
    fun onPrepaymentAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(prepaymentAmount = value, error = null)
    }
    fun onPrepaymentAfterMonthChange(value: String) {
        _uiState.value = _uiState.value.copy(prepaymentAfterMonth = value, error = null)
    }
    fun onStrategyChange(strategy: PrepaymentStrategy) {
        _uiState.value = _uiState.value.copy(prepaymentStrategy = strategy, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val loanAmount = state.loanAmount.toDoubleOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()
        val tenure = state.tenureMonths.toIntOrNull()

        if (loanAmount == null || rate == null || tenure == null) {
            _uiState.value = state.copy(error = EmiValidationError.InvalidInput, result = null)
            return
        }

        val prepaymentAmount = if (state.enablePrepayment) state.prepaymentAmount.toDoubleOrNull() else null
        val prepaymentMonth = if (state.enablePrepayment) state.prepaymentAfterMonth.toIntOrNull() else null

        if (state.enablePrepayment && (prepaymentAmount == null || prepaymentMonth == null)) {
            _uiState.value = state.copy(error = EmiValidationError.InvalidInput, result = null)
            return
        }

        try {
            val result = calculateEmi(
                EmiInput(
                    loanAmount = loanAmount,
                    interestRatePercent = rate,
                    tenureMonths = tenure,
                    prepaymentAmount = prepaymentAmount,
                    prepaymentAfterMonth = prepaymentMonth,
                    prepaymentStrategy = state.prepaymentStrategy
                )
            )
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = EmiValidationError.InvalidValue(e.message), result = null)
        }
    }
}
