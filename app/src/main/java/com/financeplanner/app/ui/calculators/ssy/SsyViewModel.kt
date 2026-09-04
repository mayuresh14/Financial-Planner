package com.financeplanner.app.ui.calculators.ssy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.model.SsyInput
import com.financeplanner.app.domain.model.SsyResult
import com.financeplanner.app.domain.usecase.CalculateSsyUseCase
import com.financeplanner.app.ui.navigation.PendingTabNavigator
import com.financeplanner.app.ui.navigation.Routes
import com.financeplanner.app.ui.navigation.TabRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SsyValidationError {
    data object InvalidInput : SsyValidationError
    data class InvalidValue(val rawMessage: String?) : SsyValidationError
}

data class SsyUiState(
    val girlAgeAtOpening: String = "",
    val yearlyDeposit: String = "",
    val interestRatePercent: String = "8.2",
    val inflationPercent: String = "",
    val existingBalance: String = "",
    val result: SsyResult? = null,
    val showResultSheet: Boolean = false,
    val error: SsyValidationError? = null,
    val showSaveSheet: Boolean = false,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val customName: String = "",
    val institutionName: String = "",
    val notes: String = ""
)

@HiltViewModel
class SsyViewModel @Inject constructor(
    private val calculateSsy: CalculateSsyUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SsyUiState())
    val uiState: StateFlow<SsyUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                savedInvestmentRepository.getById(editId)?.let { saved ->
                    val fields = SavedInvestmentJson.decode(saved.detailsJson)
                    _uiState.value = _uiState.value.copy(
                        girlAgeAtOpening = fields["girlAgeAtOpening"] ?: "",
                        yearlyDeposit = fields["yearlyDeposit"] ?: "",
                        interestRatePercent = fields["interestRatePercent"] ?: "",
                        inflationPercent = fields["inflationPercent"] ?: "",
                        existingBalance = fields["existingBalance"] ?: "",
                        editingId = saved.id,
                        customName = saved.customName,
                        institutionName = saved.institutionName ?: "",
                        notes = saved.notes ?: ""
                    )
                    return@launch
                }
            }
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(inflationPercent = prefs.defaultInflationPercent.toString())
        }
    }

    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(girlAgeAtOpening = value, error = null)
    }
    fun onDepositChange(value: String) {
        _uiState.value = _uiState.value.copy(yearlyDeposit = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(interestRatePercent = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }
    fun onExistingBalanceChange(value: String) {
        _uiState.value = _uiState.value.copy(existingBalance = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null, showResultSheet = false)
    }

    fun onSaveClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        _uiState.value = _uiState.value.copy(showSaveSheet = true)
    }

    fun onSaveDirectClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        if (computeResult() != null) {
            _uiState.value = _uiState.value.copy(showResultSheet = false, showSaveSheet = true)
        }
    }

    fun onSaveSheetDismissed() {
        _uiState.value = _uiState.value.copy(showSaveSheet = false)
    }

    fun onSaveConfirmed(customName: String, institutionName: String?, notes: String?) {
        val state = _uiState.value
        val detailsJson = SavedInvestmentJson.encode(
            mapOf(
                "girlAgeAtOpening" to state.girlAgeAtOpening,
                "yearlyDeposit" to state.yearlyDeposit,
                "interestRatePercent" to state.interestRatePercent,
                "inflationPercent" to state.inflationPercent,
                "existingBalance" to state.existingBalance
            )
        )
        viewModelScope.launch {
            savedInvestmentRepository.save(
                SavedInvestment(
                    id = state.editingId ?: 0,
                    type = SavedInvestmentType.SSY,
                    customName = customName,
                    institutionName = institutionName,
                    notes = notes,
                    detailsJson = detailsJson,
                    lastComputedValue = state.result?.maturityValue,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        pendingTabNavigator.requestTab(TabRoutes.INVESTMENTS)
        _uiState.value = state.copy(showSaveSheet = false, saveCompleted = true)
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        if (computeResult() != null) {
            _uiState.value = _uiState.value.copy(showResultSheet = true)
            analytics.logResultViewed(CALCULATOR_NAME)
        }
    }

    private fun computeResult(): SsyResult? {
        val state = _uiState.value
        val age = state.girlAgeAtOpening.toIntOrNull()
        val deposit = state.yearlyDeposit.toDoubleOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val existingBalance = state.existingBalance.toDoubleOrNull() ?: 0.0

        if (age == null || deposit == null || rate == null) {
            _uiState.value = state.copy(error = SsyValidationError.InvalidInput, result = null)
            return null
        }
        return try {
            val result = calculateSsy(SsyInput(age, deposit, rate, inflationPercent = inflation, existingBalance = existingBalance))
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SsyValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "ssy"
    }
}
