package com.financeplanner.app.ui.home

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
import androidx.compose.runtime.LaunchedEffect
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
import com.financeplanner.app.ui.common.DefaultRatesSetupSheet
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.TourOverlay
import kotlinx.coroutines.launch

/**
 * Dashboard listing all 14 calculators as per-category carousels (see
 * CalculatorCatalog for the category groupings) rather than one big grid.
 * Every card is tappable and navigable. First launch shows the app tour
 * (see TourOverlay), then — once that's dismissed or finished — a one-time
 * sheet to collect default inflation/return rates (see DefaultRatesSetupSheet)
 * that every calculator prefills from. The disclaimer banner is pinned at
 * the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCalculator: (String) -> Unit,
    settingsViewModel: AppSettingsViewModel = hiltViewModel()
) {
    val preferences by settingsViewModel.preferences.collectAsState()
    val hasLoadedPreferences by settingsViewModel.hasLoadedPreferences.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val notAvailableMessage = stringResource(R.string.calc_not_available_yet)

    // App tour temporarily hidden at startup (not removed — flip this back to
    // true to re-enable). Default-rates setup still shows regardless, since
    // it's a separate first-launch feature.
    val isAppTourEnabled = false

    var showTour by remember { mutableStateOf(false) }
    var showDefaultRatesSetup by remember { mutableStateOf(false) }
    LaunchedEffect(hasLoadedPreferences) {
        // Gated on hasLoadedPreferences (not just preferences.hasSeenAppTour)
        // so this doesn't fire on the synthetic "not seen" default that
        // `preferences` starts with before the real DataStore value loads —
        // otherwise the tour would flash/reshow on every launch.
        if (hasLoadedPreferences) {
            if (isAppTourEnabled && !preferences.hasSeenAppTour) {
                showTour = true
                settingsViewModel.logAppTourShown()
            } else if (!preferences.hasSetDefaultRates) {
                showDefaultRatesSetup = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
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
            DisclaimerBanner()
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

    if (showTour) {
        TourOverlay(onFinished = {
            showTour = false
            settingsViewModel.setHasSeenAppTour(true)
            if (!preferences.hasSetDefaultRates) showDefaultRatesSetup = true
        })
    }

    if (showDefaultRatesSetup) {
        DefaultRatesSetupSheet(
            initialInflationPercent = preferences.defaultInflationPercent,
            initialExpectedReturnPercent = preferences.defaultExpectedReturnPercent,
            onSave = { inflation, expectedReturn ->
                settingsViewModel.setDefaultInflationPercent(inflation)
                settingsViewModel.setDefaultExpectedReturnPercent(expectedReturn)
                settingsViewModel.setHasSetDefaultRates(true)
                showDefaultRatesSetup = false
            }
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
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
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
private fun DisclaimerBanner() {
    Text(
        text = stringResource(R.string.disclaimer_banner),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    )
}

@Composable
private fun CalculatorCard(item: CalculatorCatalogItem, onClick: () -> Unit) {
    val isAvailable = item.route != null
    val contentAlpha = if (isAvailable) 1f else 0.5f

    // Fixed neutral card color (not derived from the theme's primary tint,
    // unlike the page background) so the card reads as a distinct light/dark
    // "island" regardless of which preset is active, rather than blending
    // into a same-hue background.
    val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (isDarkBackground) Color(0xFF2A2A2A) else Color.White
    val onCardColor = if (isDarkBackground) Color.White else Color.Black

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
