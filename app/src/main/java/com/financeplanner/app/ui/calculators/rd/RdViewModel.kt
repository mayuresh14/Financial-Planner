package com.financeplanner.app.ui.calculators.rd

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.RdInput
import com.financeplanner.app.domain.model.RdResult
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.domain.usecase.CalculateRdUseCase
import com.financeplanner.app.ui.common.addMonths
import com.financeplanner.app.ui.common.formatDate
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

sealed interface RdValidationError {
    data object InvalidInput : RdValidationError
    data class InvalidValue(val rawMessage: String?) : RdValidationError
}

data class RdUiState(
    val monthlyDeposit: String = "",
    val annualRatePercent: String = "",
    val tenureMonths: String = "",
    val inflationPercent: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val result: RdResult? = null,
    val showResultSheet: Boolean = false,
    val error: RdValidationError? = null,
    val showSaveSheet: Boolean = false,
    val saveCompleted: Boolean = false,
    val editingId: Long? = null,
    val customName: String = "",
    val institutionName: String = "",
    val notes: String = ""
) {
    val endDateMillis: Long?
        get() = tenureMonths.toIntOrNull()?.let { addMonths(startDateMillis, it) }
}

@HiltViewModel
class RdViewModel @Inject constructor(
    private val calculateRd: CalculateRdUseCase,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val savedInvestmentRepository: SavedInvestmentRepository,
    private val pendingTabNavigator: PendingTabNavigator,
    private val analytics: AppAnalytics,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RdUiState())
    val uiState: StateFlow<RdUiState> = _uiState.asStateFlow()

    init {
        analytics.logCalculatorOpened(CALCULATOR_NAME)
        val editId = savedStateHandle.get<Long>(Routes.EDIT_ITEM_ID_ARG) ?: Routes.NO_EDIT_ITEM_ID
        viewModelScope.launch {
            if (editId != Routes.NO_EDIT_ITEM_ID) {
                savedInvestmentRepository.getById(editId)?.let { saved ->
                    val fields = SavedInvestmentJson.decode(saved.detailsJson)
                    _uiState.value = _uiState.value.copy(
                        monthlyDeposit = fields["monthlyDeposit"] ?: "",
                        annualRatePercent = fields["annualRatePercent"] ?: "",
                        tenureMonths = fields["tenureMonths"] ?: "",
                        inflationPercent = fields["inflationPercent"] ?: "",
                        startDateMillis = fields["startDateMillis"]?.toLongOrNull() ?: System.currentTimeMillis(),
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

    fun onDepositChange(value: String) {
        _uiState.value = _uiState.value.copy(monthlyDeposit = value, error = null)
    }
    fun onRateChange(value: String) {
        _uiState.value = _uiState.value.copy(annualRatePercent = value, error = null)
    }
    fun onTenureChange(value: String) {
        _uiState.value = _uiState.value.copy(tenureMonths = value, error = null)
    }
    fun onInflationChange(value: String) {
        _uiState.value = _uiState.value.copy(inflationPercent = value, error = null)
    }
    fun onStartDateChange(millis: Long) {
        _uiState.value = _uiState.value.copy(startDateMillis = millis, error = null)
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
                "monthlyDeposit" to state.monthlyDeposit,
                "annualRatePercent" to state.annualRatePercent,
                "tenureMonths" to state.tenureMonths,
                "inflationPercent" to state.inflationPercent,
                "startDate" to formatDate(state.startDateMillis),
                "startDateMillis" to state.startDateMillis.toString(),
                "endDate" to (state.endDateMillis?.let { formatDate(it) } ?: ""),
                "endDateMillis" to (state.endDateMillis?.toString() ?: "")
            )
        )
        viewModelScope.launch {
            savedInvestmentRepository.save(
                SavedInvestment(
                    id = state.editingId ?: 0,
                    type = SavedInvestmentType.RD,
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

    private fun computeResult(): RdResult? {
        val state = _uiState.value
        val deposit = state.monthlyDeposit.toDoubleOrNull()
        val rate = state.annualRatePercent.toDoubleOrNull()
        val tenure = state.tenureMonths.toIntOrNull()
        val inflation = state.inflationPercent.toDoubleOrNull()

        if (deposit == null || rate == null || tenure == null) {
            _uiState.value = state.copy(error = RdValidationError.InvalidInput, result = null)
            return null
        }
        return try {
            val result = calculateRd(
                RdInput(
                    monthlyDeposit = deposit,
                    annualRatePercent = rate,
                    tenureMonths = tenure,
                    inflationPercent = inflation
                )
            )
            _uiState.value = state.copy(result = result, error = null)
            result
        } catch (e: IllegalArgumentException) {
            analytics.recordException(e, CALCULATOR_NAME)
            _uiState.value = state.copy(error = RdValidationError.InvalidValue(e.message), result = null)
            null
        }
    }

    private companion object {
        const val CALCULATOR_NAME = "rd"
    }
}
