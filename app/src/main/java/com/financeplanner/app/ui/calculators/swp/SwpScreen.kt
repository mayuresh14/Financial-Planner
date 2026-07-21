package com.financeplanner.app.ui.calculators.swp

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.financeplanner.app.domain.model.SwpResult
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
fun SwpScreen(
    onBack: () -> Unit,
    viewModel: SwpViewModel = hiltViewModel(),
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
                title = { Text(stringResource(R.string.calc_swp)) },
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
                text = stringResource(R.string.swp_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AmountOutlinedTextField(
                value = state.initialCorpus, onValueChange = viewModel::onCorpusChange,
                label = { Text(stringResource(R.string.swp_label_corpus)) },
                modifier = Modifier.fillMaxWidth()
            )
            AmountOutlinedTextField(
                value = state.monthlyWithdrawal, onValueChange = viewModel::onWithdrawalChange,
                label = { Text(stringResource(R.string.swp_label_withdrawal)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.expectedReturnPercent, onValueChange = viewModel::onReturnChange,
                    label = { Text(stringResource(R.string.sip_label_expected_return)) },
                    modifier = Modifier.width(160.dp), singleLine = true
                )
                OutlinedTextField(
                    value = state.simulateYears, onValueChange = viewModel::onSimulateYearsChange,
                    label = { Text(stringResource(R.string.swp_label_years)) },
                    modifier = Modifier.width(160.dp), singleLine = true
                )
            }

            state.error?.let { error ->
                val message = when (error) {
                    is SwpValidationError.InvalidInput -> stringResource(R.string.sip_error_invalid_input)
                    is SwpValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.sip_error_invalid_input)
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
                state.result?.let { SwpResultCard(it, state) }
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
private fun SwpResultCard(result: SwpResult, state: SwpUiState) {
    val currencyFormat = remember(result) { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val narrative = if (result.depletionMonth != null) {
            stringResource(
                R.string.swp_result_narrative_depleted,
                formatAmountWithWords(state.monthlyWithdrawal.toDoubleOrNull() ?: 0.0, currencyFormat),
                formatAmountWithWords(state.initialCorpus.toDoubleOrNull() ?: 0.0, currencyFormat),
                state.expectedReturnPercent,
                result.depletionMonth
            )
        } else {
            stringResource(
                R.string.swp_result_narrative_survives,
                formatAmountWithWords(state.monthlyWithdrawal.toDoubleOrNull() ?: 0.0, currencyFormat),
                formatAmountWithWords(state.initialCorpus.toDoubleOrNull() ?: 0.0, currencyFormat),
                state.expectedReturnPercent,
                state.simulateYears,
                formatAmountWithWords(result.totalWithdrawn, currencyFormat),
                formatAmountWithWords(result.finalBalance, currencyFormat)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.swp_result_depletion), style = MaterialTheme.typography.labelLarge)
                if (result.depletionMonth != null) {
                    Text(
                        text = stringResource(R.string.swp_result_depletion_value, result.depletionMonth),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(R.string.swp_result_no_depletion),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ResultRow(stringResource(R.string.swp_result_total_withdrawn), currencyFormat.format(result.totalWithdrawn))
                ResultRow(stringResource(R.string.swp_result_final_balance), currencyFormat.format(result.finalBalance))
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
