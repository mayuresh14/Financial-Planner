package com.financeplanner.app.ui.calculators.assetallocation

import androidx.lifecycle.ViewModel
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.domain.model.AggressivenessLevel
import com.financeplanner.app.domain.model.AssetAllocationInput
import com.financeplanner.app.domain.model.AssetAllocationResult
import com.financeplanner.app.domain.usecase.CalculateAssetAllocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface AssetAllocationValidationError {
    data object InvalidInput : AssetAllocationValidationError
    data class InvalidValue(val rawMessage: String?) : AssetAllocationValidationError
}

data class AssetAllocationUiState(
    val currentAge: String = "",
    val retirementAge: String = "",
    val aggressiveness: AggressivenessLevel = AggressivenessLevel.MODERATE,
    val includeCrypto: Boolean = false,
    val result: AssetAllocationResult? = null,
    val error: AssetAllocationValidationError? = null
)

@HiltViewModel
class AssetAllocationViewModel @Inject constructor(
    private val calculateAssetAllocation: CalculateAssetAllocationUseCase,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetAllocationUiState())
    val uiState: StateFlow<AssetAllocationUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
    }

    fun onCurrentAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(currentAge = value, error = null)
    }
    fun onRetirementAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(retirementAge = value, error = null)
    }
    fun onAggressivenessChange(level: AggressivenessLevel) {
        _uiState.value = _uiState.value.copy(aggressiveness = level, error = null)
    }
    fun onIncludeCryptoChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(includeCrypto = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        val state = _uiState.value
        val currentAge = state.currentAge.toIntOrNull()
        val retirementAge = state.retirementAge.toIntOrNull()

        if (currentAge == null || retirementAge == null) {
            _uiState.value = state.copy(error = AssetAllocationValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateAssetAllocation(
                AssetAllocationInput(currentAge, retirementAge, state.aggressiveness, state.includeCrypto)
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = AssetAllocationValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "asset_allocation"
    }
}
