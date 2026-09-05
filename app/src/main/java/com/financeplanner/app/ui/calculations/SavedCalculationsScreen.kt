package com.financeplanner.app.ui.calculations

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.domain.model.SavedCalculation
import com.financeplanner.app.domain.model.SavedCalculationType
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.buildSavedItemNarrative
import com.financeplanner.app.ui.common.formatFieldLabel
import com.financeplanner.app.ui.common.formatFieldValue
import com.financeplanner.app.ui.common.machineOnlyFieldKeys
import com.financeplanner.app.ui.navigation.Routes
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCalculationsScreen(
    onEditCalculation: (String) -> Unit = {},
    viewModel: SavedCalculationsViewModel = hiltViewModel(),
    settingsViewModel: AppSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_calculations_title)) },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_icon_description))
                    }
                }
            )
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.saved_calculations_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.saved_calculations_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.saved_calculations_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (state.availableTypes.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.typeFilter == null,
                                onClick = { viewModel.onTypeFilterChange(null) },
                                label = { Text(stringResource(R.string.my_investments_filter_all)) }
                            )
                        }
                        items(state.availableTypes) { type ->
                            FilterChip(
                                selected = state.typeFilter == type,
                                onClick = { viewModel.onTypeFilterChange(type) },
                                label = { Text(typeLabel(type)) }
                            )
                        }
                    }
                }

                if (state.filteredItems.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.my_investments_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredItems, key = { it.id }) { item ->
                            SavedCalculationCard(
                                item = item,
                                onClick = { viewModel.onItemClicked(item) },
                                modifier = Modifier.animateItem(placementSpec = tween(300))
                            )
                        }
                    }
                }
            }
        }
    }

    state.selectedItem?.let { item ->
        CalculationDetailSheet(
            item = item,
            onDismiss = viewModel::onDetailDismissed,
            onDeleteClick = { viewModel.onDeleteRequested(item) },
            onEditClick = {
                viewModel.onEditRequested(item)
                viewModel.onDetailDismissed()
                onEditCalculation(Routes.editRoute(routeBaseFor(item.type), item.id))
            }
        )
    }

    state.pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteCancelled,
            title = { Text(stringResource(R.string.my_investments_delete_confirm_title)) },
            text = { Text(stringResource(R.string.my_investments_delete_confirm_message, item.customName)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) {
                    Text(stringResource(R.string.my_investments_delete_confirm_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteCancelled) {
                    Text(stringResource(R.string.my_investments_cancel_button))
                }
            }
        )
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

private fun routeBaseFor(type: SavedCalculationType): String = when (type) {
    SavedCalculationType.SIP -> Routes.SIP
    SavedCalculationType.LUMPSUM -> Routes.LUMPSUM
    SavedCalculationType.GOAL_BASED_SIP -> Routes.GOAL_BASED_SIP
}

@Composable
private fun typeLabel(type: SavedCalculationType): String = when (type) {
    SavedCalculationType.SIP -> stringResource(R.string.investment_type_sip)
    SavedCalculationType.LUMPSUM -> stringResource(R.string.investment_type_lumpsum)
    SavedCalculationType.GOAL_BASED_SIP -> stringResource(R.string.calc_goal_based_sip)
}

@Composable
private fun SavedCalculationCard(item: SavedCalculation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.customName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = typeLabel(item.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item.lastComputedValue?.let { value ->
                Text(
                    text = "${stringResource(R.string.my_investments_current_value)}: ${currencyFormat.format(value)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationDetailSheet(item: SavedCalculation, onDismiss: () -> Unit, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val fields = remember(item.detailsJson) { SavedInvestmentJson.decode(item.detailsJson) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.customName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.my_investments_edit_button))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.my_investments_delete_confirm_button))
                    }
                }
            }
            Text(text = typeLabel(item.type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            item.lastComputedValue?.let { value ->
                Text(
                    text = "${stringResource(R.string.my_investments_current_value)}: ${currencyFormat.format(value)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            var expanded by remember(item.id) { mutableStateOf(false) }
            val narrative = remember(item.detailsJson, item.lastComputedValue) {
                buildSavedItemNarrative(item.type.name, fields, item.lastComputedValue)
            }

            if (!expanded) {
                TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.saved_item_show_details))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                }
            } else {
                narrative?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }

                fields.filterKeys { it !in machineOnlyFieldKeys }.filterValues { it.isNotBlank() }.forEach { (key, value) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = formatFieldLabel(key), style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.7f))
                        Text(text = formatFieldValue(value), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                TextButton(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.saved_item_hide_details))
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                }
            }

            item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(text = notes, style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                text = dateFormat.format(Date(item.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }
    }
}
