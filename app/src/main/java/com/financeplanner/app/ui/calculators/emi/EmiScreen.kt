package com.financeplanner.app.ui.calculators.emi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.financeplanner.app.domain.model.EmiResult
import com.financeplanner.app.domain.model.PrepaymentStrategy
import com.financeplanner.app.ui.common.AmountOutlinedTextField
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ComingSoonSheet
import com.financeplanner.app.ui.common.NarrativeResultCard
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.formatAmountWithWords
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiScreen(
    onBack: () -> Unit,
    viewModel: EmiViewModel = hiltViewModel(),
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
                title = { Text(stringResource(R.string.calc_emi)) },
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
                text = stringResource(R.string.emi_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AmountOutlinedTextField(
                value = state.loanAmount, onValueChange = viewModel::onLoanAmountChange,
                label = { Text(stringResource(R.string.emi_label_loan_amount)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.interestRatePercent, onValueChange = viewModel::onRateChange,
                    label = { Text(stringResource(R.string.emi_label_rate)) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = state.tenureMonths, onValueChange = viewModel::onTenureChange,
                    label = { Text(stringResource(R.string.emi_label_tenure)) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.emi_toggle_prepayment), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.enablePrepayment, onCheckedChange = viewModel::onTogglePrepayment)
            }

            AnimatedVisibility(visible = state.enablePrepayment) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AmountOutlinedTextField(
                            value = state.prepaymentAmount, onValueChange = viewModel::onPrepaymentAmountChange,
                            label = { Text(stringResource(R.string.emi_label_prepayment_amount)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.prepaymentAfterMonth, onValueChange = viewModel::onPrepaymentAfterMonthChange,
                            label = { Text(stringResource(R.string.emi_label_prepayment_month)) },
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.prepaymentStrategy == PrepaymentStrategy.REDUCE_TENURE,
                            onClick = { viewModel.onStrategyChange(PrepaymentStrategy.REDUCE_TENURE) },
                            label = { Text(stringResource(R.string.emi_strategy_reduce_tenure)) }
                        )
                        FilterChip(
                            selected = state.prepaymentStrategy == PrepaymentStrategy.REDUCE_EMI,
                            onClick = { viewModel.onStrategyChange(PrepaymentStrategy.REDUCE_EMI) },
                            label = { Text(stringResource(R.string.emi_strategy_reduce_emi)) }
                        )
                    }
                }
            }

            state.error?.let { error ->
                val message = when (error) {
                    is EmiValidationError.InvalidInput -> stringResource(R.string.emi_error_invalid_input)
                    is EmiValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.emi_error_invalid_input)
                }
                Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = viewModel::calculate, modifier = Modifier.fillMaxWidth()) {
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
                state.result?.let { EmiResultCard(it, state) }
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
private fun EmiResultCard(result: EmiResult, state: EmiUiState) {
    val currencyFormat = remember(result) { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val narrative = buildString {
            append(
                stringResource(
                    R.string.emi_result_narrative,
                    formatAmountWithWords(state.loanAmount.toDoubleOrNull() ?: 0.0, currencyFormat),
                    state.interestRatePercent,
                    state.tenureMonths,
                    formatAmountWithWords(result.emi, currencyFormat),
                    formatAmountWithWords(result.totalInterestWithoutPrepayment, currencyFormat)
                )
            )
            if (result.interestSaved != null) {
                when (state.prepaymentStrategy) {
                    PrepaymentStrategy.REDUCE_TENURE -> result.monthsSaved?.let { months ->
                        append(
                            stringResource(
                                R.string.emi_result_narrative_reduce_tenure_addendum,
                                formatAmountWithWords(result.interestSaved, currencyFormat),
                                months
                            )
                        )
                    }
                    PrepaymentStrategy.REDUCE_EMI -> result.newEmiAfterPrepayment?.let { newEmi ->
                        append(
                            stringResource(
                                R.string.emi_result_narrative_reduce_emi_addendum,
                                formatAmountWithWords(result.interestSaved, currencyFormat),
                                formatAmountWithWords(newEmi, currencyFormat)
                            )
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.emi_result_emi), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = currencyFormat.format(result.emi),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ResultRow(stringResource(R.string.emi_result_total_interest), currencyFormat.format(result.totalInterestWithoutPrepayment))

                if (result.totalInterestWithPrepayment != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    result.interestSaved?.let {
                        ResultRow(stringResource(R.string.emi_result_interest_saved), currencyFormat.format(it))
                    }
                    result.monthsSaved?.let {
                        ResultRow(stringResource(R.string.emi_result_months_saved, it), "")
                    }
                    result.newEmiAfterPrepayment?.let {
                        ResultRow(stringResource(R.string.emi_result_new_emi), currencyFormat.format(it))
                    }
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
        if (value.isNotEmpty()) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
