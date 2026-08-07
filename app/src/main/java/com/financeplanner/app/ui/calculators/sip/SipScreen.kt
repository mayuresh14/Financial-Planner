package com.financeplanner.app.ui.calculators.sip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.SipFrequency
import com.financeplanner.app.domain.model.SipResult
import com.financeplanner.app.ui.common.AmountOutlinedTextField
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ComingSoonSheet
import com.financeplanner.app.ui.common.FieldHelpIcon
import com.financeplanner.app.ui.common.NarrativeResultCard
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.formatAmountWithWords
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * SIP calculator screen — the template every other calculator screen follows:
 * a scrollable form of OutlinedTextFields, a Calculate button, a result
 * bottom sheet, and a settings icon opening the shared ThemeLanguageSheet.
 * Labels are all string resources so this screen is fully localized.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipScreen(
    onBack: () -> Unit,
    viewModel: SipViewModel = hiltViewModel(),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calc_sip)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_icon_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.sip_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val frequencies = listOf(
                    SipFrequency.DAILY to R.string.sip_frequency_daily,
                    SipFrequency.WEEKLY to R.string.sip_frequency_weekly,
                    SipFrequency.MONTHLY to R.string.sip_frequency_monthly
                )
                frequencies.forEachIndexed { index, (frequency, labelRes) ->
                    SegmentedButton(
                        selected = state.frequency == frequency,
                        onClick = { viewModel.onFrequencyChange(frequency) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = frequencies.size)
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            AmountOutlinedTextField(
                value = state.monthlyAmount,
                onValueChange = viewModel::onMonthlyAmountChange,
                label = { Text(stringResource(R.string.sip_label_investment_amount)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.expectedReturnPercent,
                onValueChange = viewModel::onExpectedReturnChange,
                label = { Text(stringResource(R.string.sip_label_expected_return)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.durationYears,
                onValueChange = viewModel::onDurationChange,
                label = { Text(stringResource(R.string.sip_label_duration)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.inflationPercent,
                onValueChange = viewModel::onInflationChange,
                label = { Text(stringResource(R.string.sip_label_inflation)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = { FieldHelpIcon(stringResource(R.string.help_inflation_rate)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sip_advanced_options_toggle),
                    style = MaterialTheme.typography.titleSmall
                )
                Switch(
                    checked = state.showAdvancedOptions,
                    onCheckedChange = { viewModel.onToggleAdvancedOptions() }
                )
            }

            AnimatedVisibility(visible = state.showAdvancedOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountOutlinedTextField(
                        value = state.initialLumpsum,
                        onValueChange = viewModel::onInitialLumpsumChange,
                        label = { Text(stringResource(R.string.sip_label_initial_lumpsum)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.stepUpMode == StepUpMode.PERCENTAGE,
                            onClick = { viewModel.onStepUpModeChange(StepUpMode.PERCENTAGE) },
                            label = { Text(stringResource(R.string.sip_stepup_mode_percentage)) }
                        )
                        FilterChip(
                            selected = state.stepUpMode == StepUpMode.FIXED_AMOUNT,
                            onClick = { viewModel.onStepUpModeChange(StepUpMode.FIXED_AMOUNT) },
                            label = { Text(stringResource(R.string.sip_stepup_mode_fixed)) }
                        )
                    }

                    if (state.stepUpMode == StepUpMode.PERCENTAGE) {
                        OutlinedTextField(
                            value = state.stepUpPercent,
                            onValueChange = viewModel::onStepUpChange,
                            label = { Text(stringResource(R.string.sip_label_step_up)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = { FieldHelpIcon(stringResource(R.string.help_step_up)) }
                        )
                    } else {
                        AmountOutlinedTextField(
                            value = state.stepUpFixedAmount,
                            onValueChange = viewModel::onStepUpFixedAmountChange,
                            label = { Text(stringResource(R.string.sip_label_stepup_fixed_amount)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.help_stepup_fixed_amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = state.expenseRatioPercent,
                        onValueChange = viewModel::onExpenseRatioChange,
                        label = { Text(stringResource(R.string.sip_label_expense_ratio)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = { FieldHelpIcon(stringResource(R.string.help_expense_ratio)) }
                    )
                }
            }

            state.error?.let { error ->
                val message = when (error) {
                    is SipValidationError.InvalidInput -> stringResource(R.string.sip_error_invalid_input)
                    is SipValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.sip_error_invalid_input)
                }
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = viewModel::calculate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sip_button_calculate))
            }
        }
    }

    if (state.result != null) {
        ModalBottomSheet(
            onDismissRequest = ::dismissResultSheet,
            sheetState = resultSheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.result?.let { SipResultCard(it, state) }
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
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (state.showComingSoonSheet) {
        ComingSoonSheet(onDismiss = viewModel::onComingSoonDismissed)
    }
}

@Composable
private fun SipResultCard(result: SipResult, state: SipUiState) {
    val currencyFormat = remember(result) {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val frequencyAdverbRes = when (state.frequency) {
            SipFrequency.DAILY -> R.string.sip_frequency_adverb_daily
            SipFrequency.WEEKLY -> R.string.sip_frequency_adverb_weekly
            SipFrequency.MONTHLY -> R.string.sip_frequency_adverb_monthly
        }
        val narrative = buildString {
            append(
                stringResource(
                    R.string.sip_result_narrative,
                    formatAmountWithWords(state.monthlyAmount.toDoubleOrNull() ?: 0.0, currencyFormat),
                    stringResource(frequencyAdverbRes),
                    state.durationYears,
                    state.expectedReturnPercent,
                    formatAmountWithWords(result.maturityValue, currencyFormat)
                )
            )
            state.initialLumpsum.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                append(
                    stringResource(
                        R.string.sip_result_narrative_lumpsum_addendum,
                        formatAmountWithWords(it, currencyFormat)
                    )
                )
            }
            if (state.stepUpMode == StepUpMode.PERCENTAGE) {
                state.stepUpPercent.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                    append(stringResource(R.string.sip_result_narrative_stepup_percentage_addendum, state.stepUpPercent))
                }
            } else {
                state.stepUpFixedAmount.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                    append(
                        stringResource(
                            R.string.sip_result_narrative_stepup_fixed_addendum,
                            formatAmountWithWords(it, currencyFormat)
                        )
                    )
                }
            }
            result.expenseAmount?.let { expenseAmount ->
                append(
                    stringResource(
                        R.string.sip_result_narrative_expense_addendum,
                        state.expenseRatioPercent,
                        formatAmountWithWords(expenseAmount, currencyFormat)
                    )
                )
            }
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
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.sip_result_maturity_value),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = currencyFormat.format(result.maturityValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ResultRow(stringResource(R.string.sip_result_total_invested), currencyFormat.format(result.totalInvested))
                ResultRow(stringResource(R.string.sip_result_wealth_gained), currencyFormat.format(result.wealthGained))

                result.expenseAmount?.let { expenseAmount ->
                    ResultRow(
                        stringResource(R.string.sip_result_expense_cost),
                        currencyFormat.format(expenseAmount)
                    )
                }

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
