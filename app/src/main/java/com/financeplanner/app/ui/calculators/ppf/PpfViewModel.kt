package com.financeplanner.app.ui.calculators.ppf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.PpfInput
import com.financeplanner.app.domain.model.PpfResult
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.usecase.CalculatePpfUseCase
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

sealed interface PpfValidationError {
    data object InvalidInput : PpfValidationError
    data class InvalidValue(val rawMessage: String?) : PpfValidationError
}

/** PPF's yearly-contribution rule (≤ ₹1,50,000/year) doesn't change — this is purely an input
 * convenience so someone who thinks in "how much do I put in each month" doesn't have to do the
 * ×12 math themselves. Both modes end up feeding the same yearly figure into [PpfInput]. */
enum class PpfContributionFrequency { YEARLY, MONTHLY }

data class PpfUiState(
    val contributionFrequency: PpfContributionFrequency = PpfContributionFrequency.YEARLY,
    val yearlyContribution: String = "",
    val monthlyContribution: String = "",
    val durationYears: String = "",
    val interestRatePercent: String = "7.1",
    val inflationPercent: String = "",
    val existingBalance: String = "",
    val result: PpfResult? = null,
    val showResultSheet: Boolean = false,
    val error: PpfValidationError? = null,
    val showSaveSheet: Boolean = false,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val customName: String = "",
    val institutionName: String = "",
    val notes: String = ""
) {
    /** The figure actually fed into [PpfInput], regardless of which field the user typed into. */
    val effectiveYearlyContribution: Double?
        get() = when (contributionFrequency) {
            PpfContributionFrequency.YEARLY -> yearlyContribution.toDoubleOrNull()
            PpfContributionFrequency.MONTHLY -> monthlyContribution.toDoubleOrNull()?.let { it * 12 }
        }
}

@HiltViewModel
class PpfViewModel @Inject constructor(
    private val calculatePpf: CalculatePpfUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PpfUiState())
    val uiState: StateFlow<PpfUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                savedInvestmentRepository.getById(editId)?.let { saved ->
                    val fields = SavedInvestmentJson.decode(saved.detailsJson)
                    _uiState.value = _uiState.value.copy(
                        contributionFrequency = fields["contributionFrequency"]
                            ?.let { runCatching { PpfContributionFrequency.valueOf(it) }.getOrNull() }
                            ?: PpfContributionFrequency.YEARLY,
                        yearlyContribution = fields["yearlyContribution"] ?: "",
                        monthlyContribution = fields["monthlyContribution"] ?: "",
                        durationYears = fields["durationYears"] ?: "",
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

    fun onContributionFrequencyChange(frequency: PpfContributionFrequency) {
        _uiState.value = _uiState.value.copy(contributionFrequency = frequency, error = null)
    }
    fun onYearlyContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(yearlyContribution = value, error = null)
    }
    fun onMonthlyContributionChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyContribution = value, error = null)
    }
    fun onDurationChange(value: String) {
        _uiState.value = _uiState.value.copy(durationYears = value, error = null)
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
                "contributionFrequency" to state.contributionFrequency.name,
                "yearlyContribution" to state.yearlyContribution,
                "monthlyContribution" to state.monthlyContribution,
                "durationYears" to state.durationYears,
                "interestRatePercent" to state.interestRatePercent,
                "inflationPercent" to state.inflationPercent,
                "existingBalance" to state.existingBalance
            )
        )
        viewModelScope.launch {
            savedInvestmentRepository.save(
                SavedInvestment(
                    id = state.editingId ?: 0,
                    type = SavedInvestmentType.PPF,
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

    private fun computeResult(): PpfResult? {
        val state = _uiState.value
        val yearlyContribution = state.effectiveYearlyContribution
        val duration = state.durationYears.toIntOrNull()
        val rate = state.interestRatePercent.toDoubleOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()
        val existingBalance = state.existingBalance.toDoubleOrNull() ?: 0.0

        if (yearlyContribution == null || duration == null || rate == null) {
            _uiState.value = state.copy(error = PpfValidationError.InvalidInput, result = null)
            return null
        }
        return try {
            val result = calculatePpf(PpfInput(yearlyContribution, duration, rate, inflation, existingBalance))
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = PpfValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "ppf"
    }
}
