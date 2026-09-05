package com.financeplanner.app.ui.calculators.sip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedCalculationRepository
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.SaveTarget
import com.financeplanner.app.domain.model.SavedCalculation
import com.financeplanner.app.domain.model.SavedCalculationType
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.model.SipFrequency
import com.financeplanner.app.domain.model.SipInput
import com.financeplanner.app.domain.model.SipResult
import com.financeplanner.app.domain.usecase.CalculateSipUseCase
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

/**
 * Validation errors as a sealed type rather than a raw string — the
 * ViewModel has no Context/string-resource access, so the UI layer resolves
 * these to a localized message via stringResource(). Keeps the ViewModel
 * localization-agnostic and unit-testable without Robolectric.
 */
sealed interface SipValidationError {
    data object InvalidInput : SipValidationError
    data class InvalidValue(val rawMessage: String?) : SipValidationError
}

/** Step-up is either a percentage or a flat rupee amount added yearly — never both at once. */
enum class StepUpMode { PERCENTAGE, FIXED_AMOUNT }

/**
 * Form + result state for the SIP calculator screen. All fields are kept as
 * raw strings for TextField binding; parsing/validation happens on Calculate.
 * This is the state shape every other calculator's ViewModel will mirror.
 */
data class SipUiState(
    val monthlyAmount: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "",
    val frequency: SipFrequency = SipFrequency.MONTHLY,
    val showAdvancedOptions: Boolean = false,
    val stepUpMode: StepUpMode = StepUpMode.PERCENTAGE,
    val stepUpPercent: String = "",
    val stepUpFixedAmount: String = "",
    val expenseRatioPercent: String = "",
    val inflationPercent: String = "",
    val initialLumpsum: String = "",
    val result: SipResult? = null,
    val showResultSheet: Boolean = false,
    val error: SipValidationError? = null,
    val showSaveSheet: Boolean = false,
    val showSaveTargetChooser: Boolean = false,
    val saveTarget: SaveTarget? = null,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val editingKind: SaveTarget? = null,
    val customName: String = "",
    val institutionName: String = "",
    val notes: String = "",
    val assetCategory: AssetCategory = AssetCategory.MUTUAL_FUND
)

@HiltViewModel
class SipViewModel @Inject constructor(
    private val calculateSip: CalculateSipUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedCalculationRepository: SavedCalculationRepository,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SipUiState())
    val uiState: StateFlow<SipUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        val editKind = savedStateHandle.get<String>(Routes.EDIT_ITEM_KIND_ARG)
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                if (editKind == Routes.ITEM_KIND_INVESTMENT) {
                    savedInvestmentRepository.getById(editId)?.let { saved ->
                        applyLoadedFields(
                            fields = SavedInvestmentJson.decode(saved.detailsJson),
                            editingId = saved.id,
                            editingKind = SaveTarget.INVESTMENT,
                            customName = saved.customName,
                            institutionName = saved.institutionName ?: "",
                            notes = saved.notes ?: ""
                        )
                        return@launch
                    }
                } else {
                    savedCalculationRepository.getById(editId)?.let { saved ->
                        applyLoadedFields(
                            fields = SavedInvestmentJson.decode(saved.detailsJson),
                            editingId = saved.id,
                            editingKind = SaveTarget.CALCULATION,
                            customName = saved.customName,
                            institutionName = "",
                            notes = saved.notes ?: ""
                        )
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
            monthlyAmount = fields["monthlyAmount"] ?: "",
            expectedReturnPercent = fields["expectedReturnPercent"] ?: "",
            durationYears = fields["durationYears"] ?: "",
            frequency = fields["frequency"]?.let { runCatching { SipFrequency.valueOf(it) }.getOrNull() } ?: SipFrequency.MONTHLY,
            stepUpMode = fields["stepUpMode"]?.let { runCatching { StepUpMode.valueOf(it) }.getOrNull() } ?: StepUpMode.PERCENTAGE,
            stepUpPercent = fields["stepUpPercent"] ?: "",
            stepUpFixedAmount = fields["stepUpFixedAmount"] ?: "",
            expenseRatioPercent = fields["expenseRatioPercent"] ?: "",
            inflationPercent = fields["inflationPercent"] ?: "",
            initialLumpsum = fields["initialLumpsum"] ?: "",
            assetCategory = fields["assetCategory"]?.let { runCatching { AssetCategory.valueOf(it) }.getOrNull() } ?: AssetCategory.MUTUAL_FUND,
            editingId = editingId,
            editingKind = editingKind,
            customName = customName,
            institutionName = institutionName,
            notes = notes
        )
    }

    fun onMonthlyAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyAmount = value, error = null)
    }

    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }

    fun onFrequencyChange(frequency: SipFrequency) {
        _uiState.value = _uiState.value.copy(frequency = frequency, error = null)
    }

    fun onToggleAdvancedOptions() {
        _uiState.value = _uiState.value.copy(showAdvancedOptions = !_uiState.value.showAdvancedOptions)
    }

    fun onStepUpModeChange(mode: StepUpMode) {
        _uiState.value = _uiState.value.copy(stepUpMode = mode, error = null)
    }

    fun onStepUpChange(value: String) {
        _uiState.value = _uiState.value.copy(stepUpPercent = value, error = null)
    }

    fun onStepUpFixedAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(stepUpFixedAmount = value, error = null)
    }

    fun onExpenseRatioChange(value: String) {
        _uiState.value = _uiState.value.copy(expenseRatioPercent = value, error = null)
    }

    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun onInitialLumpsumChange(value: String) {
        _uiState.value = _uiState.value.copy(initialLumpsum = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null, showResultSheet = false)
    }

    fun onSaveClicked() {
        analytics.logSaveTapped(CALCULATOR_NAME)
        proceedToSave()
    }

    /** "Save" tapped directly from the form, without viewing the result sheet first —
     * still computes the result (needed for lastComputedValue), just skips showing it. */
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
            // Editing an existing item — its kind is already fixed, skip the chooser.
            _uiState.value = state.copy(saveTarget = state.editingKind, showSaveSheet = true)
        } else {
            _uiState.value = state.copy(showSaveTargetChooser = true)
        }
    }

    fun onSaveTargetChosen(target: SaveTarget) {
        analytics.logSaveTargetChosen(CALCULATOR_NAME, target.name)
        _uiState.value = _uiState.value.copy(saveTarget = target, showSaveTargetChooser = false, showSaveSheet = true)
    }

    fun onSaveTargetChooserDismissed() {
        _uiState.value = _uiState.value.copy(showSaveTargetChooser = false)
    }

    fun onSaveSheetDismissed() {
        _uiState.value = _uiState.value.copy(showSaveSheet = false)
    }

    fun onSaveConfirmed(customName: String, institutionName: String?, notes: String?, assetCategory: AssetCategory?) {
        val state = _uiState.value
        val detailsJson = SavedInvestmentJson.encode(
            mapOf(
                "monthlyAmount" to state.monthlyAmount,
                "expectedReturnPercent" to state.expectedReturnPercent,
                "durationYears" to state.durationYears,
                "frequency" to state.frequency.name,
                "stepUpMode" to state.stepUpMode.name,
                "stepUpPercent" to state.stepUpPercent,
                "stepUpFixedAmount" to state.stepUpFixedAmount,
                "expenseRatioPercent" to state.expenseRatioPercent,
                "inflationPercent" to state.inflationPercent,
                "initialLumpsum" to state.initialLumpsum,
                "assetCategory" to (assetCategory ?: state.assetCategory).name
            )
        )
        viewModelScope.launch {
            if (state.saveTarget == SaveTarget.INVESTMENT) {
                savedInvestmentRepository.save(
                    SavedInvestment(
                        id = state.editingId ?: 0,
                        type = SavedInvestmentType.SIP,
                        customName = customName,
                        institutionName = institutionName,
                        notes = notes,
                        detailsJson = detailsJson,
                        lastComputedValue = state.result?.maturityValue,
                        createdAt = System.currentTimeMillis()
                    )
                )
                pendingTabNavigator.requestTab(TabRoutes.INVESTMENTS)
            } else {
                savedCalculationRepository.save(
                    SavedCalculation(
                        id = state.editingId ?: 0,
                        type = SavedCalculationType.SIP,
                        customName = customName,
                        notes = notes,
                        detailsJson = detailsJson,
                        lastComputedValue = state.result?.maturityValue,
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

    fun calculate() {
        analytics.logCalculateTapped(CALCULATOR_NAME)
        if (computeResult() != null) {
            _uiState.value = _uiState.value.copy(showResultSheet = true)
            analytics.logResultViewed(CALCULATOR_NAME)
        }
    }

    private fun computeResult(): SipResult? {
        val state = _uiState.value
        val monthlyAmount = state.monthlyAmount.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val stepUpPercent = if (state.stepUpMode == StepUpMode.PERCENTAGE) state.stepUpPercent.toDoubleOrNull() else null
        val stepUpFixedAmount = if (state.stepUpMode == StepUpMode.FIXED_AMOUNT) state.stepUpFixedAmount.toDoubleOrNull() else null
        val expenseRatio = state.expenseRatioPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val initialLumpsum = state.initialLumpsum.toDoubleOrNull()

        if (monthlyAmount == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = SipValidationError.InvalidInput, result = null)
            return null
        }

        return try {
            val input = SipInput(
                contributionAmount = monthlyAmount,
                expectedReturnPercent = expectedReturn,
                durationYears = duration,
                frequency = state.frequency,
                stepUpPercent = stepUpPercent,
                stepUpFixedAmount = stepUpFixedAmount,
                expenseRatioPercent = expenseRatio,
                inflationPercent = inflation,
                initialLumpsum = initialLumpsum
            )
            val result = calculateSip(input)
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = SipValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "sip"
    }
}
