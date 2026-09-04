package com.financeplanner.app.ui.calculators.tenure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.financeplanner.app.domain.model.TenureResult
import com.financeplanner.app.ui.common.AmountOutlinedTextField
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.NarrativeResultCard
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.formatAmountWithWords
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenureScreen(
    onBack: () -> Unit,
    viewModel: TenureViewModel = hiltViewModel(),
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
                title = { Text(stringResource(R.string.calc_tenure)) },
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
                text = stringResource(R.string.tenure_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AmountOutlinedTextField(
                value = state.monthlyAmount, onValueChange = viewModel::onMonthlyAmountChange,
                label = { Text(stringResource(R.string.sip_label_monthly_amount)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.expectedReturnPercent, onValueChange = viewModel::onExpectedReturnChange,
                label = { Text(stringResource(R.string.sip_label_expected_return)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            AmountOutlinedTextField(
                value = state.targetAmount, onValueChange = viewModel::onTargetAmountChange,
                label = { Text(stringResource(R.string.tenure_label_target_amount)) },
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let { error ->
                val message = when (error) {
                    is TenureValidationError.InvalidInput -> stringResource(R.string.tenure_error_invalid_input)
                    is TenureValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.tenure_error_invalid_input)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.result?.let { TenureResultCard(it, state) }
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
}

@Composable
private fun TenureResultCard(result: TenureResult, state: TenureUiState) {
    val currencyFormat = remember(result) { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.tenure_result_years), style = MaterialTheme.typography.labelLarge)
                if (result.yearsRequired != null) {
                    Text(
                        text = stringResource(R.string.tenure_result_years_value, result.yearsRequired),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.tenure_error_unreachable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (result.yearsRequired != null) {
            val narrative = stringResource(
                R.string.tenure_result_narrative,
                formatAmountWithWords(state.monthlyAmount.toDoubleOrNull() ?: 0.0, currencyFormat),
                state.expectedReturnPercent,
                stringResource(R.string.tenure_result_years_value, result.yearsRequired),
                formatAmountWithWords(state.targetAmount.toDoubleOrNull() ?: 0.0, currencyFormat)
            )
            NarrativeResultCard(narrative)
        }
    }
}
