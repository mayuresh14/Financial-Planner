package com.financeplanner.app.ui.calculators.alcohol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.AlcoholInput
import com.financeplanner.app.domain.model.AlcoholResult
import com.financeplanner.app.domain.usecase.CalculateAlcoholUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlcoholValidationError {
    data object InvalidInput : AlcoholValidationError
    data class InvalidValue(val rawMessage: String?) : AlcoholValidationError
}

data class AlcoholUiState(
    val drinksPerWeek: String = "",
    val costPerDrink: String = "",
    val years: String = "",
    val expectedReturnPercent: String = "",
    val result: AlcoholResult? = null,
    val error: AlcoholValidationError? = null
)

@HiltViewModel
class AlcoholViewModel @Inject constructor(
    private val calculateAlcohol: CalculateAlcoholUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlcoholUiState())
    val uiState: StateFlow<AlcoholUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString()
            )
        }
    }

    fun onDrinksPerWeekChange(value: String) {
        _uiState.value = _uiState.value.copy(drinksPerWeek = value, error = null)
    }
    fun onCostPerDrinkChange(value: String) {
        _uiState.value = _uiState.value.copy(costPerDrink = value, error = null)
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
        val drinksPerWeek = state.drinksPerWeek.toIntOrNull()
        val costPerDrink = state.costPerDrink.toDoubleOrNull()
        val years = state.years.toIntOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()

        if (drinksPerWeek == null || costPerDrink == null || years == null || expectedReturn == null) {
            _uiState.value = state.copy(error = AlcoholValidationError.InvalidInput, result = null)
            return
        }
        try {
            val result = calculateAlcohol(
                AlcoholInput(
                    drinksPerWeek = drinksPerWeek,
                    costPerDrink = costPerDrink,
                    years = years,
                    expectedReturnPercent = expectedReturn
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            analytics.logResultViewed(CALCULATOR_NAME)
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = AlcoholValidationError.InvalidValue(e.message), result = null)
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "alcohol"
    }
}
