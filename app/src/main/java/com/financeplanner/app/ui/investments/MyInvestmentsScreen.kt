package com.financeplanner.app.ui.investments

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.financeplanner.app.R
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.domain.model.InvestmentsViewMode
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.assetCategoryLabel
import com.financeplanner.app.ui.common.assetCategoryOrNull
import com.financeplanner.app.ui.common.buildSavedItemNarrative
import com.financeplanner.app.ui.common.ExportViewModel
import com.financeplanner.app.ui.common.InvestmentAnalysis
import com.financeplanner.app.ui.common.computeInvestmentAnalysis
import com.financeplanner.app.ui.common.formatFieldLabel
import com.financeplanner.app.ui.common.formatFieldValue
import com.financeplanner.app.ui.common.investmentTypeLabel
import com.financeplanner.app.ui.common.machineOnlyFieldKeys
import com.financeplanner.app.ui.navigation.Routes
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyInvestmentsScreen(
    onBack: (() -> Unit)?,
    onAddInvestment: (String) -> Unit = {},
    onEditInvestment: (String) -> Unit = {},
    viewModel: MyInvestmentsViewModel = hiltViewModel(),
    settingsViewModel: AppSettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(state.searchQuery.isNotBlank()) }
    var showExportEmptyMessage by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Keeps the search field visible whenever there's an active query — e.g. after
    // switching tabs and back, when this composable (and its local `showSearch`) is
    // recreated but the ViewModel's searchQuery survives, so results stay filtered
    // with no visible box to see or clear the query from.
    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery.isNotBlank()) showSearch = true
    }

    fun closeSearch() {
        showSearch = false
        viewModel.onSearchQueryChange("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_investments_title)) },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onViewModeToggled() }) {
                        Icon(
                            if (state.viewMode == InvestmentsViewMode.LIST) Icons.Filled.Menu else Icons.Filled.List,
                            contentDescription = stringResource(R.string.my_investments_view_toggle)
                        )
                    }
                    IconButton(onClick = { if (showSearch) closeSearch() else showSearch = true }) {
                        Icon(
                            if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = stringResource(R.string.my_investments_search_placeholder)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.onExportTriggered()
                        coroutineScope.launch {
                            val shareIntent = exportViewModel.buildExportShareIntent()
                            if (shareIntent != null) {
                                context.startActivity(shareIntent)
                            } else {
                                showExportEmptyMessage = true
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.settings_export_investments_action))
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_icon_description))
                    }
                }
            )
        },
        floatingActionButton = {
            // Default FAB color is primaryContainer, which (now that it's a light wash
            // rather than a saturated tone) barely stands out against a white background —
            // full-strength primary instead so it's unmistakable.
            FloatingActionButton(
                onClick = {
                    viewModel.onAddInvestmentMenuOpened()
                    showAddMenu = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.my_investments_add_button))
            }
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.my_investments_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.my_investments_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else if (state.viewMode == InvestmentsViewMode.SUMMARY) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showSearch) {
                    LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                    InvestmentsSearchField(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClose = { closeSearch() },
                        focusRequester = searchFocusRequester
                    )
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
                    InvestmentsSummaryView(
                        analysis = computeInvestmentAnalysis(state.filteredItems),
                        rows = state.typeSummaryRows,
                        items = state.filteredItems,
                        onItemClick = { viewModel.onItemClicked(it) },
                        onSectionToggled = { type, expanded -> viewModel.onSummarySectionToggled(type, expanded) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showSearch) {
                    LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                    InvestmentsSearchField(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClose = { closeSearch() },
                        focusRequester = searchFocusRequester
                    )
                }
                if (state.availableTypes.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.typeFilters.isEmpty(),
                                onClick = { viewModel.onTypeFilterToggle(null) },
                                label = { Text(stringResource(R.string.my_investments_filter_all)) }
                            )
                        }
                        items(state.availableTypes) { type ->
                            FilterChip(
                                selected = type in state.typeFilters,
                                onClick = { viewModel.onTypeFilterToggle(type) },
                                label = { Text(investmentTypeLabel(type)) }
                            )
                        }
                    }
                }

                if (state.availableAssetCategories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.assetCategoryFilters.isEmpty(),
                                onClick = { viewModel.onAssetCategoryFilterToggle(null) },
                                label = { Text(stringResource(R.string.my_investments_filter_asset_category_all)) }
                            )
                        }
                        items(state.availableAssetCategories) { category ->
                            FilterChip(
                                selected = category in state.assetCategoryFilters,
                                onClick = { viewModel.onAssetCategoryFilterToggle(category) },
                                label = { Text(assetCategoryLabel(category)) }
                            )
                        }
                    }
                }

                // Deliberately outside the LazyColumn — a fixed summary strip above the list,
                // not another row that scrolls away with everything else.
                FilteredSummaryCard(
                    count = state.filteredItems.size,
                    analysis = computeInvestmentAnalysis(state.filteredItems),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

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
                            SavedInvestmentCard(
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
        InvestmentDetailSheet(
            item = item,
            onDismiss = viewModel::onDetailDismissed,
            onDeleteClick = { viewModel.onDeleteRequested(item) },
            onEditClick = {
                viewModel.onEditRequested(item)
                viewModel.onDetailDismissed()
                onEditInvestment(editRouteFor(item.type, item.id))
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

    if (showExportEmptyMessage) {
        AlertDialog(
            onDismissRequest = { showExportEmptyMessage = false },
            title = { Text(stringResource(R.string.settings_export_empty_title)) },
            text = { Text(stringResource(R.string.settings_export_empty_message)) },
            confirmButton = {
                TextButton(onClick = { showExportEmptyMessage = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    if (showAddMenu) {
        AddInvestmentMenuSheet(
            onDismiss = { showAddMenu = false },
            onSelect = { route ->
                showAddMenu = false
                viewModel.onAddInvestmentTypeSelected(route)
                onAddInvestment(route)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInvestmentMenuSheet(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = listOf(
        R.string.investment_type_sip to Routes.SIP,
        R.string.investment_type_lumpsum to Routes.LUMPSUM,
        R.string.calc_goal_based_sip to Routes.GOAL_BASED_SIP,
        R.string.investment_type_fd to Routes.FD,
        R.string.investment_type_rd to Routes.RD,
        R.string.investment_type_ppf to Routes.PPF,
        R.string.investment_type_epf to Routes.EPF,
        R.string.calc_ssy to Routes.SSY,
        R.string.investment_type_nps to Routes.NPS
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.my_investments_add_menu_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            options.forEach { (labelRes, route) ->
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onSelect(route) })
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

private fun routeBaseFor(type: SavedInvestmentType): String = when (type) {
    SavedInvestmentType.FD -> Routes.FD
    SavedInvestmentType.RD -> Routes.RD
    SavedInvestmentType.PPF -> Routes.PPF
    SavedInvestmentType.EPF -> Routes.EPF
    SavedInvestmentType.SSY -> Routes.SSY
    SavedInvestmentType.NPS -> Routes.NPS
    SavedInvestmentType.SIP -> Routes.SIP
    SavedInvestmentType.LUMPSUM -> Routes.LUMPSUM
    SavedInvestmentType.GOAL_BASED_SIP -> Routes.GOAL_BASED_SIP
}

private fun isDualKindType(type: SavedInvestmentType): Boolean = when (type) {
    SavedInvestmentType.SIP, SavedInvestmentType.LUMPSUM, SavedInvestmentType.GOAL_BASED_SIP -> true
    else -> false
}

private fun editRouteFor(type: SavedInvestmentType, id: Long): String {
    val base = routeBaseFor(type)
    return if (isDualKindType(type)) {
        Routes.editRouteWithKind(base, id, Routes.ITEM_KIND_INVESTMENT)
    } else {
        Routes.editRoute(base, id)
    }
}

/** Summarizes whatever's currently visible under the active search/filter — not the whole
 * portfolio (that's the Home dashboard's job) — so it updates live as filters change. */
@Composable
private fun FilteredSummaryCard(count: Int, analysis: InvestmentAnalysis, modifier: Modifier = Modifier) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.my_investments_summary_count, count),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (analysis.totalProjectedValue > 0) {
                Text(
                    text = stringResource(R.string.my_investments_summary_total, currencyFormat.format(analysis.totalProjectedValue)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (analysis.totalMonthlyCommitment > 0) {
                Text(
                    text = "${stringResource(R.string.investments_analysis_monthly_commitment)}: ${currencyFormat.format(analysis.totalMonthlyCommitment)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** Shared by both List and Summary view — search narrows results in either mode. */
@Composable
private fun InvestmentsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.my_investments_search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.my_investments_cancel_button))
            }
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester)
    )
}

/** Portfolio-wide overview: total strip + one collapsible section per type (count, combined
 * value), richest first — tapping a section expands it in place to show its items, rather than
 * navigating away. Alternative to scrolling a long flat list once there are many saved items. */
@Composable
private fun InvestmentsSummaryView(
    analysis: InvestmentAnalysis,
    rows: List<TypeSummaryRow>,
    items: List<SavedInvestment>,
    onItemClick: (SavedInvestment) -> Unit,
    onSectionToggled: (SavedInvestmentType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedTypes by remember { mutableStateOf(setOf<SavedInvestmentType>()) }

    Column(modifier = modifier) {
        FilteredSummaryCard(
            count = items.size,
            analysis = analysis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { row ->
                val expanded = row.type in expandedTypes
                item(key = row.type) {
                    TypeSummaryCard(
                        row = row,
                        expanded = expanded,
                        onClick = {
                            val nowExpanded = !expanded
                            expandedTypes = if (expanded) expandedTypes - row.type else expandedTypes + row.type
                            onSectionToggled(row.type, nowExpanded)
                        },
                        modifier = Modifier.animateItem(placementSpec = tween(300))
                    )
                }
                if (expanded) {
                    items(items.filter { it.type == row.type }, key = { it.id }) { item ->
                        SavedInvestmentCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .animateItem(placementSpec = tween(300))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeSummaryCard(row: TypeSummaryRow, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = investmentTypeLabel(row.type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.my_investments_summary_count, row.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.totalValue?.takeIf { it > 0 }?.let { value ->
                    Text(
                        text = currencyFormat.format(value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun SavedInvestmentCard(item: SavedInvestment, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                val typeText = item.assetCategoryOrNull()?.let { "${investmentTypeLabel(item.type)} · ${assetCategoryLabel(it)}" }
                    ?: investmentTypeLabel(item.type)
                Text(
                    text = typeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item.institutionName?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.75f))
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
private fun InvestmentDetailSheet(item: SavedInvestment, onDismiss: () -> Unit, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
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
            val typeText = item.assetCategoryOrNull()?.let { "${investmentTypeLabel(item.type)} · ${assetCategoryLabel(it)}" }
                ?: investmentTypeLabel(item.type)
            Text(text = typeText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            item.institutionName?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            item.lastComputedValue?.let { value ->
                Text(
                    text = "${stringResource(R.string.my_investments_current_value)}: ${currencyFormat.format(value)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            var expanded by remember(item.id) { mutableStateOf(true) }
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
