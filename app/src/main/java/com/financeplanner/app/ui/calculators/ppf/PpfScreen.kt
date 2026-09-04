package com.financeplanner.app.ui.calculators.ppf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.PpfResult
import com.financeplanner.app.ui.common.AmountOutlinedTextField
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.FieldHelpIcon
import com.financeplanner.app.ui.common.NarrativeResultCard
import com.financeplanner.app.ui.common.SaveCompletedEffect
import com.financeplanner.app.ui.common.SaveInvestmentSheet
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.formatAmountWithWords
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpfScreen(
    onBack: () -> Unit,
    viewModel: PpfViewModel = hiltViewModel(),
    settingsViewModel: AppSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    val resultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun dismissResultSheet() {
        coroutineScope.launch { resultSheetState.hide() }.invokeOnCompletion {
            if (!resultSheetState.isVisible) viewModel.onResultDismissed()
        }
    }

    SaveCompletedEffect(state.saveCompleted, viewModel::onSaveCompletedHandled, onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calc_ppf)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_icon_description))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.ppf_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val frequencies = listOf(
                    PpfContributionFrequency.YEARLY to R.string.ppf_contribution_frequency_yearly,
                    PpfContributionFrequency.MONTHLY to R.string.ppf_contribution_frequency_monthly
                )
                frequencies.forEachIndexed { index, (frequency, labelRes) ->
                    SegmentedButton(
                        selected = state.contributionFrequency == frequency,
                        onClick = { viewModel.onContributionFrequencyChange(frequency) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = frequencies.size)
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }
            if (state.contributionFrequency == PpfContributionFrequency.MONTHLY) {
                AmountOutlinedTextField(
                    value = state.monthlyContribution, onValueChange = viewModel::onMonthlyContributionChange,
                    label = { Text(stringResource(R.string.ppf_label_monthly_contribution)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AmountOutlinedTextField(
                    value = state.yearlyContribution, onValueChange = viewModel::onYearlyContributionChange,
                    label = { Text(stringResource(R.string.ppf_label_yearly_contribution)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AmountOutlinedTextField(
                value = state.existingBalance, onValueChange = viewModel::onExistingBalanceChange,
                label = { Text(stringResource(R.string.label_existing_balance)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { FieldHelpIcon(stringResource(R.string.help_existing_balance)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.durationYears, onValueChange = viewModel::onDurationChange,
                    label = { Text(stringResource(R.string.ppf_label_duration)) },
                    modifier = Modifier.width(180.dp), singleLine = true
                )
                OutlinedTextField(
                    value = state.interestRatePercent, onValueChange = viewModel::onRateChange,
                    label = { Text(stringResource(R.string.ppf_label_rate)) },
                    modifier = Modifier.width(140.dp), singleLine = true,
                    trailingIcon = { FieldHelpIcon(stringResource(R.string.help_ppf_rate)) }
                )
            }

            OutlinedTextField(
                value = state.inflationPercent, onValueChange = viewModel::onInflationChange,
                label = { Text(stringResource(R.string.sip_label_inflation)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                trailingIcon = { FieldHelpIcon(stringResource(R.string.help_inflation_rate)) }
            )

            state.error?.let { error ->
                val message = when (error) {
                    is PpfValidationError.InvalidInput -> stringResource(R.string.ppf_error_invalid_input)
                    is PpfValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.ppf_error_invalid_input)
                }
                Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = viewModel::calculate, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sip_button_calculate))
                }
                OutlinedButton(onClick = viewModel::onSaveDirectClicked, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sip_button_save))
                }
            }
        }
    }

    if (state.result != null && state.showResultSheet) {
        ModalBottomSheet(
            onDismissRequest = ::dismissResultSheet,
            sheetState = resultSheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.result?.let { PpfResultCard(it, state) }
                Button(onClick = viewModel::onSaveClicked, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sip_button_save))
                }
            }
        }
    }

    if (showSettingsSheet) {
        ThemeLanguageSheet(
            preferences = preferences,
            onThemeModeChange = settingsViewModel::setThemeMode,
            onThemePresetChange = settingsViewModel::setThemePreset,
            onRandomizeTheme = settingsViewModel::randomizeTheme,
            onLanguageChange = settingsViewModel::setLanguage,
            onDefaultInflationChange = settingsViewModel::setDefaultInflationPercent,
            onDefaultExpectedReturnChange = settingsViewModel::setDefaultExpectedReturnPercent,
            onUserNameChange = settingsViewModel::setUserName,
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (state.showSaveSheet) {
        SaveInvestmentSheet(
            onDismiss = viewModel::onSaveSheetDismissed,
            onSave = { name, inst, notes, _ -> viewModel.onSaveConfirmed(name, inst, notes) },
            initialCustomName = state.customName,
            initialInstitutionName = state.institutionName,
            initialNotes = state.notes
        )
    }
}

@Composable
private fun PpfResultCard(result: PpfResult, state: PpfUiState) {
    val currencyFormat = remember(result) { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val narrative = buildString {
            append(
                stringResource(
                    R.string.ppf_result_narrative,
                    formatAmountWithWords(state.effectiveYearlyContribution ?: 0.0, currencyFormat),
                    state.durationYears,
                    state.interestRatePercent,
                    formatAmountWithWords(result.maturityValue, currencyFormat),
                    formatAmountWithWords(result.interestEarned, currencyFormat)
                )
            )
            result.inflationAdjustedValue?.let {
                append(stringResource(R.string.narrative_inflation_addendum, formatAmountWithWords(it, currencyFormat)))
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.sip_result_maturity_value), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = currencyFormat.format(result.maturityValue),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ResultRow(stringResource(R.string.ppf_result_total_contributed), currencyFormat.format(result.totalContributed))
                ResultRow(stringResource(R.string.ppf_result_interest_earned), currencyFormat.format(result.interestEarned))

                result.inflationAdjustedValue?.let { adjusted ->
                    ResultRow(
                        stringResource(R.string.sip_result_inflation_adjusted),
                        currencyFormat.format(adjusted)
                    )
                }
            }
        }

        NarrativeResultCard(narrative)
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.75f)
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
