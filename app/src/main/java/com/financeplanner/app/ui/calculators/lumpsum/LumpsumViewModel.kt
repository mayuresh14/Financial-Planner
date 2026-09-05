package com.financeplanner.app.ui.calculators.lumpsum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedCalculationRepository
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.LumpsumInput
import com.financeplanner.app.domain.model.LumpsumResult
import com.financeplanner.app.domain.model.SaveTarget
import com.financeplanner.app.domain.model.SavedCalculation
import com.financeplanner.app.domain.model.SavedCalculationType
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.usecase.CalculateLumpsumUseCase
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

sealed interface LumpsumValidationError {
    data object InvalidInput : LumpsumValidationError
    data class InvalidValue(val rawMessage: String?) : LumpsumValidationError
}

data class LumpsumUiState(
    val principal: String = "",
    val expectedReturnPercent: String = "12",
    val durationYears: String = "",
    val inflationPercent: String = "",
    val result: LumpsumResult? = null,
    val showResultSheet: Boolean = false,
    val error: LumpsumValidationError? = null,
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
class LumpsumViewModel @Inject constructor(
    private val calculateLumpsum: CalculateLumpsumUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedCalculationRepository: SavedCalculationRepository,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(LumpsumUiState())
    val uiState: StateFlow<LumpsumUiState> = _uiState.asStateFlow()

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
            principal = fields["principal"] ?: "",
            expectedReturnPercent = fields["expectedReturnPercent"] ?: "",
            durationYears = fields["durationYears"] ?: "",
            inflationPercent = fields["inflationPercent"] ?: "",
            assetCategory = fields["assetCategory"]?.let { runCatching { AssetCategory.valueOf(it) }.getOrNull() } ?: AssetCategory.MUTUAL_FUND,
            editingId = editingId,
            editingKind = editingKind,
            customName = customName,
            institutionName = institutionName,
            notes = notes
        )
    }

    fun onPrincipalChange(value: String) {
        _uiState.value = _uiState.value.copy(principal = value, error = null)
    }

    fun onExpectedReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
    }

    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }

    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }

    fun onResultDismissed() {
        _uiState.value = _uiState.value.copy(result = null, showResultSheet = false)
    }

    fun onSaveClicked() {
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
                "principal" to state.principal,
                "expectedReturnPercent" to state.expectedReturnPercent,
                "durationYears" to state.durationYears,
                "inflationPercent" to state.inflationPercent,
                "assetCategory" to (assetCategory ?: state.assetCategory).name
            )
        )
        viewModelScope.launch {
            if (state.saveTarget == SaveTarget.INVESTMENT) {
                savedInvestmentRepository.save(
                    SavedInvestment(
                        id = state.editingId ?: 0,
                        type = SavedInvestmentType.LUMPSUM,
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
                        type = SavedCalculationType.LUMPSUM,
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

    private fun computeResult(): LumpsumResult? {
        val state = _uiState.value
        val principal = state.principal.toDoubleOrNull()
        val expectedReturn = state.expectedReturnPercent.toDoubleOrNull()
        val duration = state.durationYears.toIntOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (principal == null || expectedReturn == null || duration == null) {
            _uiState.value = state.copy(error = LumpsumValidationError.InvalidInput, result = null)
            return null
        }

        return try {
            val input = LumpsumInput(
                principal = principal,
                expectedReturnPercent = expectedReturn,
                durationYears = duration,
                inflationPercent = inflation
            )
            val result = calculateLumpsum(input)
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = LumpsumValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "lumpsum"
    }
}
