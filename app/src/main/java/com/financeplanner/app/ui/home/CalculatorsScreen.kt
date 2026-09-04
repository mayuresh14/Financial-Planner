package com.financeplanner.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import kotlinx.coroutines.launch

/**
 * Tab 2 of the bottom nav: the full calculator catalog, grouped into
 * per-category carousels (see CalculatorCatalog). This used to be the whole
 * Home screen; first-launch flows (local-data popup, name capture, default
 * rates) now live on DashboardScreen since that's the actual start tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    onNavigateToCalculator: (String) -> Unit,
    settingsViewModel: AppSettingsViewModel = hiltViewModel()
) {
    val preferences by settingsViewModel.preferences.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val notAvailableMessage = stringResource(R.string.calc_not_available_yet)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calculators_tab_title)) },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_icon_description)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            CalculatorCatalog.itemsByCategory.forEach { (category, catalogItems) ->
                if (catalogItems.isNotEmpty()) {
                    CategorySection(
                        titleRes = category.titleRes,
                        catalogItems = catalogItems,
                        onItemClick = { item ->
                            if (item.route != null) {
                                onNavigateToCalculator(item.route)
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(notAvailableMessage) }
                            }
                        }
                    )
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
}

@Composable
private fun CategorySection(
    titleRes: Int,
    catalogItems: List<CalculatorCatalogItem>,
    onItemClick: (CalculatorCatalogItem) -> Unit
) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 6.dp)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(catalogItems) { item ->
            CalculatorCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun CalculatorCard(item: CalculatorCatalogItem, onClick: () -> Unit) {
    val isAvailable = item.route != null
    val contentAlpha = if (isAvailable) 1f else 0.5f

    val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (isDarkBackground) Color(0xFF2A2A2A) else Color.White
    val onCardColor = if (isDarkBackground) Color.White else Color.Black

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = contentAlpha),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                color = onCardColor.copy(alpha = contentAlpha)
            )
        }
    }
}
