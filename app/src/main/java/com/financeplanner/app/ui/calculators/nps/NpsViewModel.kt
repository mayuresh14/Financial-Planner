package com.financeplanner.app.ui.calculators.nps

import androidx.lifecycle.ViewModel
import com.financeplanner.app.domain.model.NpsInput
import com.financeplanner.app.domain.model.NpsResult
import com.financeplanner.app.domain.usecase.CalculateNpsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface NpsValidationError {
    data object InvalidInput : NpsValidationError
    data class InvalidValue(val rawMessage: String?) : NpsValidationError
}

data class NpsUiState(
    val monthlyContribution: String = "",
    val currentAge: String = "30",
    val expectedReturnPercent: String = "10",
    val annuityPercent: String = "40",
    val result: NpsResult? = null,
    val error: NpsValidationError? = null
)

@HiltViewModel
class NpsViewModel @Inject constructor(
    private val calculateNps: CalculateNpsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NpsUiState())
    val uiState: StateFlow<NpsUiState> = _uiState.asStateFlow()

    fun onContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyContribution = value, error = null)
    }
    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(currentAge = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onAnnuityChange(value: String) {
        _uiState.value = _uiState.value.copy(annuityPercent = value, error = null)
    }

    fun calculate() {
        val state = _uiState.value
        val contribution = state.monthlyContribution.toDoubleOrNull()
        val age = state.currentAge.toIntOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val annuityPct = state.annuityPercent.toDoubleOrNull()

        if (contribution == null || age == null || returnPct == null || annuityPct == null) {
            _uiState.value = state.copy(error = NpsValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateNps(NpsInput(contribution, age, returnPct, annuityPct))
            _uiState.value = state.copy(result = result, error = null)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(error = NpsValidationError.InvalidValue(e.message), result = null)
        }
    }
}
