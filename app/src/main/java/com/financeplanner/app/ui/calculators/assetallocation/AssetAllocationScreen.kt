package com.financeplanner.app.ui.calculators.assetallocation

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
import com.financeplanner.app.domain.model.AggressivenessLevel
import com.financeplanner.app.domain.model.AssetAllocationResult
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ComingSoonSheet
import com.financeplanner.app.ui.common.NarrativeResultCard
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetAllocationScreen(
    onBack: () -> Unit,
    viewModel: AssetAllocationViewModel = hiltViewModel(),
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
                title = { Text(stringResource(R.string.calc_asset_allocation)) },
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
                text = stringResource(R.string.asset_allocation_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.currentAge, onValueChange = viewModel::onCurrentAgeChange,
                    label = { Text(stringResource(R.string.fire_label_current_age)) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = state.retirementAge, onValueChange = viewModel::onRetirementAgeChange,
                    label = { Text(stringResource(R.string.fire_label_retirement_age)) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }

            Text(stringResource(R.string.asset_allocation_label_aggressiveness), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val levels = listOf(
                    AggressivenessLevel.CONSERVATIVE to R.string.asset_allocation_aggressiveness_conservative,
                    AggressivenessLevel.MODERATE to R.string.asset_allocation_aggressiveness_moderate,
                    AggressivenessLevel.AGGRESSIVE to R.string.asset_allocation_aggressiveness_aggressive
                )
                levels.forEachIndexed { index, (level, labelRes) ->
                    SegmentedButton(
                        selected = state.aggressiveness == level,
                        onClick = { viewModel.onAggressivenessChange(level) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = levels.size)
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.asset_allocation_label_include_crypto),
                    style = MaterialTheme.typography.titleSmall
                )
                Switch(
                    checked = state.includeCrypto,
                    onCheckedChange = viewModel::onIncludeCryptoChange
                )
            }

            state.error?.let { error ->
                val message = when (error) {
                    is AssetAllocationValidationError.InvalidInput -> stringResource(R.string.asset_allocation_error_invalid_input)
                    is AssetAllocationValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.asset_allocation_error_invalid_input)
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
                state.result?.let { AssetAllocationResultCard(it, state) }
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
private fun aggressivenessLabel(level: AggressivenessLevel): String = when (level) {
    AggressivenessLevel.CONSERVATIVE -> stringResource(R.string.asset_allocation_aggressiveness_conservative)
    AggressivenessLevel.MODERATE -> stringResource(R.string.asset_allocation_aggressiveness_moderate)
    AggressivenessLevel.AGGRESSIVE -> stringResource(R.string.asset_allocation_aggressiveness_aggressive)
}

@Composable
private fun AssetAllocationResultCard(result: AssetAllocationResult, state: AssetAllocationUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val narrative = stringResource(
            R.string.asset_allocation_result_narrative,
            state.currentAge,
            state.retirementAge,
            aggressivenessLabel(state.aggressiveness)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.asset_allocation_result_title), style = MaterialTheme.typography.labelLarge)

                AllocationRow(stringResource(R.string.asset_allocation_bucket_equity), result.equityPercent)
                AllocationRow(stringResource(R.string.asset_allocation_bucket_debt), result.debtPercent)
                AllocationRow(stringResource(R.string.asset_allocation_bucket_gold), result.goldPercent)
                AllocationRow(stringResource(R.string.asset_allocation_bucket_emergency_fund), result.emergencyFundPercent)
                if (result.cryptoPercent > 0) {
                    AllocationRow(stringResource(R.string.asset_allocation_bucket_crypto), result.cryptoPercent)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.asset_allocation_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        NarrativeResultCard(narrative)
    }
}

@Composable
private fun AllocationRow(label: String, percent: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.85f)
        )
        Text(
            text = "${percent.roundToInt()}%",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
        )
    }
}
