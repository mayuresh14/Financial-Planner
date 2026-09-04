package com.financeplanner.app.ui.calculators.nps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.NpsInput
import com.financeplanner.app.domain.model.NpsResult
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.usecase.CalculateNpsUseCase
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

sealed interface NpsValidationError {
    data object InvalidInput : NpsValidationError
    data class InvalidValue(val rawMessage: String?) : NpsValidationError
}

data class NpsUiState(
    val monthlyContribution: String = "",
    val currentAge: String = "",
    val expectedReturnPercent: String = "10",
    val annuityPercent: String = "",
    val inflationPercent: String = "",
    val existingBalance: String = "",
    val result: NpsResult? = null,
    val showResultSheet: Boolean = false,
    val error: NpsValidationError? = null,
    val showSaveSheet: Boolean = false,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val customName: String = "",
    val institutionName: String = "",
    val notes: String = ""
)

@HiltViewModel
class NpsViewModel @Inject constructor(
    private val calculateNps: CalculateNpsUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(NpsUiState())
    val uiState: StateFlow<NpsUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                savedInvestmentRepository.getById(editId)?.let { saved ->
                    val fields = SavedInvestmentJson.decode(saved.detailsJson)
                    _uiState.value = _uiState.value.copy(
                        monthlyContribution = fields["monthlyContribution"] ?: "",
                        currentAge = fields["currentAge"] ?: "",
                        expectedReturnPercent = fields["expectedReturnPercent"] ?: "",
                        annuityPercent = fields["annuityPercent"] ?: "",
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
            _uiState.value = _uiState.value.copy(
                expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString(),
                inflationPercent = prefs.defaultInflationPercent.toString()
            )
        }
    }

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
                "monthlyContribution" to state.monthlyContribution,
                "currentAge" to state.currentAge,
                "expectedReturnPercent" to state.expectedReturnPercent,
                "annuityPercent" to state.annuityPercent,
                "inflationPercent" to state.inflationPercent,
                "existingBalance" to state.existingBalance
            )
        )
        viewModelScope.launch {
            savedInvestmentRepository.save(
                SavedInvestment(
                    id = state.editingId ?: 0,
                    type = SavedInvestmentType.NPS,
                    customName = customName,
                    institutionName = institutionName,
                    notes = notes,
                    detailsJson = detailsJson,
                    lastComputedValue = state.result?.corpusAtSixty,
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

    private fun computeResult(): NpsResult? {
        val state = _uiState.value
        val contribution = state.monthlyContribution.toDoubleOrNull()
        val age = state.currentAge.toIntOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val annuityPct = state.annuityPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val existingBalance = state.existingBalance.toDoubleOrNull() ?: 0.0

        if (contribution == null || age == null || returnPct == null || annuityPct == null) {
            _uiState.value = state.copy(error = NpsValidationError.InvalidInput, result = null)
            return null
        }
        return try {
            val result = calculateNps(NpsInput(contribution, age, returnPct, annuityPct, inflationPercent = inflation, existingBalance = existingBalance))
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = NpsValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "nps"
    }
}
