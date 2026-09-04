package com.financeplanner.app.ui.calculators.goalbasedsip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButton
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
import com.financeplanner.app.domain.model.GoalBasedSipResult
import com.financeplanner.app.domain.model.GoalType
import com.financeplanner.app.ui.common.AmountOutlinedTextField
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.domain.model.SaveTarget
import com.financeplanner.app.ui.common.SaveCalculationSheet
import com.financeplanner.app.ui.common.SaveCompletedEffect
import com.financeplanner.app.ui.common.SaveInvestmentSheet
import com.financeplanner.app.ui.common.SaveTargetChooserSheet
import com.financeplanner.app.ui.common.FieldHelpIcon
import com.financeplanner.app.ui.common.NarrativeResultCard
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.formatAmountWithWords
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalBasedSipScreen(
    onBack: () -> Unit,
    viewModel: GoalBasedSipViewModel = hiltViewModel(),
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
                title = { Text(stringResource(R.string.calc_goal_based_sip)) },
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
                text = stringResource(R.string.goal_sip_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.goalName, onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.goal_planning_label_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Text(stringResource(R.string.goal_planning_label_type), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalType.entries) { type ->
                    FilterChip(
                        selected = state.goalType == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(goalTypeLabel(type)) }
                    )
                }
            }

            AmountOutlinedTextField(
                value = state.targetAmount, onValueChange = viewModel::onTargetAmountChange,
                label = { Text(stringResource(R.string.goal_sip_label_target)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.expectedReturnPercent, onValueChange = viewModel::onExpectedReturnChange,
                label = { Text(stringResource(R.string.sip_label_expected_return)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.durationYears, onValueChange = viewModel::onDurationChange,
                    label = { Text(stringResource(R.string.sip_label_duration)) },
                    modifier = Modifier.width(160.dp), singleLine = true
                )
                OutlinedTextField(
                    value = state.inflationPercent, onValueChange = viewModel::onInflationChange,
                    label = { Text(stringResource(R.string.sip_label_inflation)) },
                    modifier = Modifier.width(160.dp), singleLine = true,
                    trailingIcon = { FieldHelpIcon(stringResource(R.string.help_inflation_rate)) }
                )
            }

            state.error?.let { error ->
                val message = when (error) {
                    is GoalBasedSipValidationError.InvalidInput -> stringResource(R.string.goal_sip_error_invalid_input)
                    is GoalBasedSipValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.goal_sip_error_invalid_input)
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
                state.result?.let { GoalBasedSipResultCard(it, state) }
                OutlinedButton(onClick = viewModel::onSaveGoalClicked, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.goal_planning_button_save))
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
        if (state.saveTarget == SaveTarget.INVESTMENT) {
            SaveInvestmentSheet(
                onDismiss = viewModel::onSaveSheetDismissed,
                onSave = { name, inst, notes, _ -> viewModel.onSaveConfirmed(name, inst, notes) },
                initialCustomName = state.savedCustomName,
                initialInstitutionName = state.institutionName,
                initialNotes = state.notes
            )
        } else {
            SaveCalculationSheet(
                onDismiss = viewModel::onSaveSheetDismissed,
                onSave = { name, notes -> viewModel.onSaveConfirmed(name, null, notes) },
                initialCustomName = state.savedCustomName,
                initialNotes = state.notes
            )
        }
    }

    if (state.showSaveTargetChooser) {
        SaveTargetChooserSheet(
            onDismiss = viewModel::onSaveTargetChooserDismissed,
            onChoose = viewModel::onSaveTargetChosen
        )
    }
}

@Composable
private fun goalTypeLabel(type: GoalType): String = when (type) {
    GoalType.RETIREMENT -> stringResource(R.string.goal_type_retirement)
    GoalType.HOUSE -> stringResource(R.string.goal_type_house)
    GoalType.EDUCATION -> stringResource(R.string.goal_type_education)
    GoalType.CAR -> stringResource(R.string.goal_type_car)
    GoalType.CUSTOM -> stringResource(R.string.goal_type_custom)
}

@Composable
private fun GoalBasedSipResultCard(result: GoalBasedSipResult, state: GoalBasedSipUiState) {
    val currencyFormat = remember(result) { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val narrative = buildString {
            append(
                stringResource(
                    R.string.goal_sip_result_narrative,
                    formatAmountWithWords(state.targetAmount.toDoubleOrNull() ?: 0.0, currencyFormat),
                    state.durationYears,
                    state.expectedReturnPercent,
                    formatAmountWithWords(result.requiredMonthlySip, currencyFormat)
                )
            )
            result.inflationAdjustedTarget?.let { target ->
                append(
                    stringResource(
                        R.string.goal_sip_result_narrative_inflation_addendum,
                        state.inflationPercent,
                        formatAmountWithWords(target, currencyFormat),
                        formatAmountWithWords(result.sipForInflationAdjustedTarget ?: 0.0, currencyFormat)
                    )
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.goal_sip_result_required_sip), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = currencyFormat.format(result.requiredMonthlySip),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                )
                result.inflationAdjustedTarget?.let { target ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.goal_sip_result_inflation_target),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.75f)
                        )
                        Text(
                            text = currencyFormat.format(target),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                        )
                    }
                    result.sipForInflationAdjustedTarget?.let { sip ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.goal_sip_result_inflation_sip),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalContentColor.current.copy(alpha = 0.75f)
                            )
                            Text(
                                text = currencyFormat.format(sip),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        NarrativeResultCard(narrative)
    }
}
