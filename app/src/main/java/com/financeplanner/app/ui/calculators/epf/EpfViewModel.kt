package com.financeplanner.app.ui.calculators.epf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.EpfInput
import com.financeplanner.app.domain.model.EpfResult
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.usecase.CalculateEpfUseCase
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

sealed interface EpfValidationError {
    data object InvalidInput : EpfValidationError
    data class InvalidValue(val rawMessage: String?) : EpfValidationError
}

/** Whether the monthly contribution is derived from salary × percentages, or typed directly —
 * useful for someone who already knows their combined EPF contribution and doesn't want to
 * reverse-engineer a salary figure to get there. */
enum class EpfContributionMode { FROM_SALARY, FLAT_AMOUNT }

data class EpfUiState(
    val contributionMode: EpfContributionMode = EpfContributionMode.FROM_SALARY,
    val basicMonthlySalary: String = "",
    val employeeContributionPercent: String = "",
    val employerContributionPercent: String = "",
    val flatMonthlyContribution: String = "",
    val durationYears: String = "",
    val expectedReturnPercent: String = "8.25",
    val inflationPercent: String = "",
    val existingBalance: String = "",
    val result: EpfResult? = null,
    val showResultSheet: Boolean = false,
    val error: EpfValidationError? = null,
    val showSaveSheet: Boolean = false,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val customName: String = "",
    val institutionName: String = "",
    val notes: String = ""
)

@HiltViewModel
class EpfViewModel @Inject constructor(
    private val calculateEpf: CalculateEpfUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpfUiState())
    val uiState: StateFlow<EpfUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                savedInvestmentRepository.getById(editId)?.let { saved ->
                    val fields = SavedInvestmentJson.decode(saved.detailsJson)
                    _uiState.value = _uiState.value.copy(
                        contributionMode = fields["contributionMode"]
                            ?.let { runCatching { EpfContributionMode.valueOf(it) }.getOrNull() }
                            ?: EpfContributionMode.FROM_SALARY,
                        basicMonthlySalary = fields["basicMonthlySalary"] ?: "",
                        employeeContributionPercent = fields["employeeContributionPercent"] ?: "",
                        employerContributionPercent = fields["employerContributionPercent"] ?: "",
                        flatMonthlyContribution = fields["flatMonthlyContribution"] ?: "",
                        durationYears = fields["durationYears"] ?: "",
                        expectedReturnPercent = fields["expectedReturnPercent"] ?: "",
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

    fun onContributionModeChange(mode: EpfContributionMode) {
        _uiState.value = _uiState.value.copy(contributionMode = mode, error = null)
    }
    fun onSalaryChange(value: String) {
        _uiState.value = _uiState.value.copy(basicMonthlySalary = value, error = null)
    }
    fun onEmployeeContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(employeeContributionPercent = value, error = null)
    }
    fun onEmployerContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(employerContributionPercent = value, error = null)
    }
    fun onFlatMonthlyContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(flatMonthlyContribution = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
    }
    fun onReturnChange(value: String) {
        _uiState.value = _uiState.value.copy(expectedReturnPercent = value, error = null)
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
                "contributionMode" to state.contributionMode.name,
                "basicMonthlySalary" to state.basicMonthlySalary,
                "employeeContributionPercent" to state.employeeContributionPercent,
                "employerContributionPercent" to state.employerContributionPercent,
                "flatMonthlyContribution" to state.flatMonthlyContribution,
                "durationYears" to state.durationYears,
                "expectedReturnPercent" to state.expectedReturnPercent,
                "inflationPercent" to state.inflationPercent,
                "existingBalance" to state.existingBalance
            )
        )
        viewModelScope.launch {
            savedInvestmentRepository.save(
                SavedInvestment(
                    id = state.editingId ?: 0,
                    type = SavedInvestmentType.EPF,
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

    private fun computeResult(): EpfResult? {
        val state = _uiState.value
        val duration = state.durationYears.toIntOrNull()
        val returnPct = state.expectedReturnPercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val existingBalance = state.existingBalance.toDoubleOrNull() ?: 0.0

        val salary: Double?
        val employeePct: Double?
        val employerPct: Double?
        val flatContribution: Double?

        if (state.contributionMode == EpfContributionMode.FLAT_AMOUNT) {
            salary = null
            employeePct = 12.0
            employerPct = 12.0
            flatContribution = state.flatMonthlyContribution.toDoubleOrNull()
            if (flatContribution == null || duration == null || returnPct == null) {
                _uiState.value = state.copy(error = EpfValidationError.InvalidInput, result = null)
                return null
            }
        } else {
            salary = state.basicMonthlySalary.toDoubleOrNull()
            employeePct = state.employeeContributionPercent.toDoubleOrNull()
            employerPct = state.employerContributionPercent.toDoubleOrNull()
            flatContribution = null
            if (salary == null || employeePct == null || employerPct == null || duration == null || returnPct == null) {
                _uiState.value = state.copy(error = EpfValidationError.InvalidInput, result = null)
                return null
            }
        }

        return try {
            val result = calculateEpf(
                EpfInput(
                    basicMonthlySalary = salary,
                    employeeContributionPercent = employeePct ?: 12.0,
                    employerContributionPercent = employerPct ?: 12.0,
                    flatMonthlyContribution = flatContribution,
                    durationYears = duration,
                    expectedReturnPercent = returnPct,
                    inflationPercent = inflation,
                    existingBalance = existingBalance
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = EpfValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "epf"
    }
}
