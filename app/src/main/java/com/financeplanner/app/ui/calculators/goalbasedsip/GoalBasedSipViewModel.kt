package com.financeplanner.app.ui.calculators.goalbasedsip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedCalculationRepository
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.GoalBasedSipInput
import com.financeplanner.app.domain.model.GoalBasedSipResult
import com.financeplanner.app.domain.model.GoalType
import com.financeplanner.app.domain.model.SaveTarget
import com.financeplanner.app.domain.model.SavedCalculation
import com.financeplanner.app.domain.model.SavedCalculationType
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.usecase.CalculateGoalBasedSipUseCase
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

sealed interface GoalBasedSipValidationError {
    data object InvalidInput : GoalBasedSipValidationError
    data class InvalidValue(val rawMessage: String?) : GoalBasedSipValidationError
}

data class GoalBasedSipUiState(
    val goalName: String = "",
    val goalType: GoalType = GoalType.RETIREMENT,
    val targetAmount: String = "",
    val durationYears: String = "",
    val expectedReturnPercent: String = "",
    val inflationPercent: String = "",
    val result: GoalBasedSipResult? = null,
    val showResultSheet: Boolean = false,
    val error: GoalBasedSipValidationError? = null,
    val showSaveSheet: Boolean = false,
    val showSaveTargetChooser: Boolean = false,
    val saveTarget: SaveTarget? = null,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val editingKind: SaveTarget? = null,
    val savedCustomName: String = "",
    val institutionName: String = "",
    val notes: String = ""
)

/**
 * Also covers what was previously a separate "Goal Planning" screen (named
 * goal + type + a gated "Save & Track" action) — that screen duplicated this
 * calculator's exact math, so it was merged in here instead of kept as a
 * second entry point. Return/inflation fields prefill from the user's saved
 * defaults (see AppPreferencesDataStore) rather than starting blank/hardcoded.
 */
@HiltViewModel
class GoalBasedSipViewModel @Inject constructor(
    private val calculateGoalBasedSip: CalculateGoalBasedSipUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedCalculationRepository: SavedCalculationRepository,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalBasedSipUiState())
    val uiState: StateFlow<GoalBasedSipUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        val editKind = savedStateHandle.get<String>(Routes.EDIT_ITEM_KIND_ARG)
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                if (editKind == Routes.ITEM_KIND_INVESTMENT) {
                    savedInvestmentRepository.getById(editId)?.let { saved ->
                        applyLoadedFields(SavedInvestmentJson.decode(saved.detailsJson), saved.id, SaveTarget.INVESTMENT, saved.customName, saved.institutionName ?: "", saved.notes ?: "")
                        return@launch
                    }
                } else {
                    savedCalculationRepository.getById(editId)?.let { saved ->
                        applyLoadedFields(SavedInvestmentJson.decode(saved.detailsJson), saved.id, SaveTarget.CALCULATION, saved.customName, "", saved.notes ?: "")
                        return@launch
                    }
                }
            }
            val prefs = preferencesDataStore.preferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                expectedReturnPercent = prefs.defaultExpectedReturnPercent.toString(),
                inflationPercent = prefs.defaultInflationPercent.toString()
            )
        }
    }

    private fun applyLoadedFields(
        fields: Map<String, String>,
        editingId: Long,
        editingKind: SaveTarget,
        customName: String,
        institutionName: String,
        notes: String
    ) {
        _uiState.value = _uiState.value.copy(
            goalName = fields["goalName"] ?: "",
            goalType = fields["goalType"]?.let { runCatching { GoalType.valueOf(it) }.getOrNull() } ?: GoalType.RETIREMENT,
            targetAmount = fields["targetAmount"] ?: "",
            durationYears = fields["durationYears"] ?: "",
            expectedReturnPercent = fields["expectedReturnPercent"] ?: "",
            inflationPercent = fields["inflationPercent"] ?: "",
            editingId = editingId,
            editingKind = editingKind,
            savedCustomName = customName,
            institutionName = institutionName,
            notes = notes
        )
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(goalName = value, error = null)
    }
    fun onTypeChange(type: GoalType) {
        _uiState.value = _uiState.value.copy(goalType = type, error = null)
    }
    fun onTargetAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(targetAmount = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null, showResultSheet = false)
    }

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        if (computeResult() != null) {
            _uiState.value = _uiState.value.copy(showResultSheet = true)
            analytics.logResultViewed(CALCULATOR_NAME)
        }
    }

    private fun computeResult(): GoalBasedSipResult? {
        val state = _uiState.value
        val targetAmount = state.targetAmount.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (targetAmount == null || duration == null || expectedReturn == null) {
            _uiState.value = state.copy(error = GoalBasedSipValidationError.InvalidInput, result = null)
            return null
        }
        return try {
            val result = calculateGoalBasedSip(
                GoalBasedSipInput(targetAmount, duration, expectedReturn, inflation)
            )
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = GoalBasedSipValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    fun onSaveGoalClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        proceedToSave()
    }

    fun onSaveDirectClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        if (computeResult() != null) {
            _uiState.value = _uiState.value.copy(showResultSheet = false)
            proceedToSave()
        }
    }

    private fun proceedToSave() {
        val state = _uiState.value
        if (state.editingKind != null) {
            _uiState.value = state.copy(saveTarget = state.editingKind, showSaveSheet = true)
        } else {
            _uiState.value = state.copy(showSaveTargetChooser = true)
        }
    }

    fun onSaveTargetChosen(target: SaveTarget) {
        _uiState.value = _uiState.value.copy(saveTarget = target, showSaveTargetChooser = false, showSaveSheet = true)
    }

    fun onSaveTargetChooserDismissed() {
        _uiState.value = _uiState.value.copy(showSaveTargetChooser = false)
    }

    fun onSaveSheetDismissed() {
        _uiState.value = _uiState.value.copy(showSaveSheet = false)
    }

    fun onSaveConfirmed(customName: String, institutionName: String?, notes: String?) {
        val state = _uiState.value
        val detailsJson = SavedInvestmentJson.encode(
            mapOf(
                "goalName" to state.goalName,
                "goalType" to state.goalType.name,
                "targetAmount" to state.targetAmount,
                "durationYears" to state.durationYears,
                "expectedReturnPercent" to state.expectedReturnPercent,
                "inflationPercent" to state.inflationPercent
            )
        )
        viewModelScope.launch {
            if (state.saveTarget == SaveTarget.INVESTMENT) {
                savedInvestmentRepository.save(
                    SavedInvestment(
                        id = state.editingId ?: 0,
                        type = SavedInvestmentType.GOAL_BASED_SIP,
                        customName = customName,
                        institutionName = institutionName,
                        notes = notes,
                        detailsJson = detailsJson,
                        lastComputedValue = state.result?.requiredMonthlySip,
                        createdAt = System.currentTimeMillis()
                    )
                )
                pendingTabNavigator.requestTab(TabRoutes.INVESTMENTS)
            } else {
                savedCalculationRepository.save(
                    SavedCalculation(
                        id = state.editingId ?: 0,
                        type = SavedCalculationType.GOAL_BASED_SIP,
                        customName = customName,
                        notes = notes,
                        detailsJson = detailsJson,
                        lastComputedValue = state.result?.requiredMonthlySip,
                        createdAt = System.currentTimeMillis()
                    )
                )
                pendingTabNavigator.requestTab(TabRoutes.CALCULATIONS)
            }
        }
        _uiState.value = state.copy(showSaveSheet = false, saveCompleted = true)
    }

    fun onSaveCompletedHandled() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    private companion object {
        const val CALCULATOR_NAME = "goal_based_sip"
    }
}
