package com.financeplanner.app.ui.calculators.fire

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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.FireResult
import com.financeplanner.app.domain.model.FireVariant
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireScreen(
    onBack: () -> Unit,
    viewModel: FireViewModel = hiltViewModel(),
    settingsViewModel: AppSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calc_fire)) },
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FireVariant.entries) { variant ->
                    FilterChip(
                        selected = state.variant == variant,
                        onClick = { viewModel.onVariantChange(variant) },
                        label = { Text(variantLabel(variant)) }
                    )
                }
            }
            Text(
                text = variantExplainer(state.variant),
                style = MaterialTheme.typography.bodySmall,
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
            OutlinedTextField(
                value = state.currentAnnualExpenses, onValueChange = viewModel::onExpensesChange,
                label = { Text(stringResource(R.string.fire_label_expenses)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.inflationPercent, onValueChange = viewModel::onInflationChange,
                    label = { Text(stringResource(R.string.fire_label_inflation)) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = state.preRetirementReturnPercent, onValueChange = viewModel::onReturnChange,
                    label = { Text(stringResource(R.string.fire_label_return)) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }
            OutlinedTextField(
                value = state.existingCorpus, onValueChange = viewModel::onExistingCorpusChange,
                label = { Text(stringResource(R.string.fire_label_existing_corpus)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            state.error?.let { error ->
                val message = when (error) {
                    is FireValidationError.InvalidInput -> stringResource(R.string.sip_error_invalid_input)
                    is FireValidationError.InvalidValue -> error.rawMessage ?: stringResource(R.string.sip_error_invalid_input)
                }
                Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = viewModel::calculate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sip_button_calculate))
            }

            AnimatedVisibility(
                visible = state.result != null,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()
            ) {
                state.result?.let { FireResultCard(it) }
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
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun variantLabel(variant: FireVariant): String = when (variant) {
    FireVariant.TRADITIONAL -> stringResource(R.string.fire_variant_traditional)
    FireVariant.LEAN -> stringResource(R.string.fire_variant_lean)
    FireVariant.FAT -> stringResource(R.string.fire_variant_fat)
    FireVariant.COAST -> stringResource(R.string.fire_variant_coast)
}

@Composable
private fun variantExplainer(variant: FireVariant): String = when (variant) {
    FireVariant.TRADITIONAL -> stringResource(R.string.fire_explainer_traditional)
    FireVariant.LEAN -> stringResource(R.string.fire_explainer_lean)
    FireVariant.FAT -> stringResource(R.string.fire_explainer_fat)
    FireVariant.COAST -> stringResource(R.string.fire_explainer_coast)
}

@Composable
private fun FireResultCard(result: FireResult) {
    val currencyFormat = remember(result) { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.fire_result_required_corpus), style = MaterialTheme.typography.labelLarge)
            Text(
                text = currencyFormat.format(result.requiredCorpus),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.fire_result_required_sip), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = currencyFormat.format(result.requiredMonthlySip),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold
                )
            }
            result.isCoastFireAchieved?.let { achieved ->
                Text(
                    text = if (achieved) stringResource(R.string.fire_result_coast_achieved)
                           else stringResource(R.string.fire_result_coast_not_achieved),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
