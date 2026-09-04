package com.financeplanner.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.AssetCategory
import com.financeplanner.app.domain.model.SavedInvestmentType
import com.financeplanner.app.ui.common.AppSettingsViewModel
import com.financeplanner.app.ui.common.AskNameSheet
import com.financeplanner.app.ui.common.AssetValueSlice
import com.financeplanner.app.ui.common.DefaultRatesSetupSheet
import com.financeplanner.app.ui.common.InvestmentAnalysis
import com.financeplanner.app.ui.common.LocalDataPopup
import com.financeplanner.app.ui.common.MAX_LOCAL_DATA_POPUP_SHOWN_COUNT
import com.financeplanner.app.ui.common.ThemeLanguageSheet
import com.financeplanner.app.ui.common.TypeValueSlice
import com.financeplanner.app.ui.common.UpcomingMaturity
import com.financeplanner.app.ui.common.assetCategoryChartColor
import com.financeplanner.app.ui.common.assetCategoryLabel
import com.financeplanner.app.ui.common.formatDate
import com.financeplanner.app.ui.common.investmentTypeChartColor
import com.financeplanner.app.ui.common.investmentTypeLabel
import java.text.NumberFormat
import kotlin.math.atan2
import kotlin.math.sqrt
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Tab 1 of the bottom nav: a personalized dashboard rather than the old
 * combined "calculator catalog + everything" screen. Calculators moved to
 * their own tab (see CalculatorsScreen); saved investments to their own tab
 * (see MyInvestmentsScreen). This screen just orients the user: who they
 * are, what they've saved, and what's coming up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInvestments: () -> Unit,
    settingsViewModel: AppSettingsViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val preferences by settingsViewModel.preferences.collectAsState()
    val hasLoadedPreferences by settingsViewModel.hasLoadedPreferences.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    var showLocalDataPopup by remember { mutableStateOf(false) }
    var showAskName by remember { mutableStateOf(false) }
    var showDefaultRatesSetup by remember { mutableStateOf(false) }

    fun proceedPastLocalDataPopup() {
        if (!preferences.hasAskedUserName) {
            showAskName = true
        } else if (!preferences.hasSetDefaultRates) {
            showDefaultRatesSetup = true
        }
    }

    fun proceedPastAskName() {
        if (!preferences.hasSetDefaultRates) {
            showDefaultRatesSetup = true
        }
    }

    LaunchedEffect(hasLoadedPreferences) {
        if (hasLoadedPreferences) {
            if (settingsViewModel.consumeLocalDataPopupSessionCheck()) {
                if (preferences.localDataPopupShownCount < MAX_LOCAL_DATA_POPUP_SHOWN_COUNT) {
                    showLocalDataPopup = true
                } else {
                    proceedPastLocalDataPopup()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_icon_description))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = (
                    preferences.userName?.takeIf { it.isNotBlank() }?.let {
                        stringResource(R.string.dashboard_greeting_named, it)
                    } ?: stringResource(R.string.dashboard_greeting_generic)
                    ) + " 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dashboardState.totalInvestments == 0) {
                EmptyInvestmentsCard(onClick = onNavigateToInvestments)
            } else {
                InvestmentsSnapshotSection(
                    analysis = dashboardState.analysis,
                    onClick = onNavigateToInvestments,
                    onTypeSliceClick = { type ->
                        dashboardViewModel.onTypeSliceClicked(type)
                        onNavigateToInvestments()
                    },
                    onAssetCategorySliceClick = { category ->
                        dashboardViewModel.onAssetCategorySliceClicked(category)
                        onNavigateToInvestments()
                    }
                )
            }

            DisclaimerBanner()
        }
    }

    if (showLocalDataPopup) {
        LocalDataPopup(
            shownCount = preferences.localDataPopupShownCount,
            onDismiss = {
                showLocalDataPopup = false
                settingsViewModel.onLocalDataPopupShown()
                proceedPastLocalDataPopup()
            }
        )
    }

    if (showAskName) {
        AskNameSheet(
            onSave = { name ->
                showAskName = false
                settingsViewModel.setUserName(name)
                proceedPastAskName()
            },
            onSkip = {
                showAskName = false
                settingsViewModel.setHasAskedUserName(true)
                proceedPastAskName()
            }
        )
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
private fun EmptyInvestmentsCard(onClick: () -> Unit) {
    // Matches the disclaimer banner's tone (surfaceVariant) rather than the more
    // saturated primaryContainer — this card is empty-state copy, not a result to
    // draw the eye to, so it shouldn't be the most visually loud thing on the screen.
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.dashboard_investments_summary_title),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(R.string.dashboard_investments_summary_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** The "at a glance" portfolio snapshot — moved here from the Investments tab so that
 * tab can stay focused on the list/search/filter, while Home stays the quick-glance screen. */
@Composable
private fun InvestmentsSnapshotSection(
    analysis: InvestmentAnalysis,
    onClick: () -> Unit,
    onTypeSliceClick: (SavedInvestmentType) -> Unit,
    onAssetCategorySliceClick: (AssetCategory) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }
    val hasAnyStats = analysis.totalMonthlyCommitment > 0 || analysis.totalLumpSumInvested > 0 || analysis.totalProjectedValue > 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hasAnyStats) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
            ) {
                AnalysisStatCard(
                    label = stringResource(R.string.investments_analysis_monthly_commitment),
                    value = currencyFormat.format(analysis.totalMonthlyCommitment),
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
                AnalysisStatCard(
                    label = stringResource(R.string.investments_analysis_lump_sum),
                    value = currencyFormat.format(analysis.totalLumpSumInvested),
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
                AnalysisStatCard(
                    label = stringResource(R.string.investments_analysis_projected_value),
                    value = currencyFormat.format(analysis.totalProjectedValue),
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (analysis.upcomingMaturities.isNotEmpty()) {
            UpcomingMaturitiesCard(analysis.upcomingMaturities, onClick = onClick)
        }

        if (analysis.typeBreakdown.isNotEmpty()) {
            PortfolioMixCard(analysis.typeBreakdown, onClick = onClick, onSliceClick = onTypeSliceClick)
        }

        if (analysis.assetCategoryBreakdown.isNotEmpty()) {
            AssetClassMixCard(analysis.assetCategoryBreakdown, onClick = onClick, onSliceClick = onAssetCategorySliceClick)
        }
    }
}

@Composable
private fun AnalysisStatCard(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.75f),
                maxLines = 2
            )
        }
    }
}

/** Which slice (by index into the caller's list) a tap landed on, or null if the tap missed
 * the ring entirely (inside the hole or outside the chart) — shared by both donut charts below. */
private fun hitTestSlice(tapOffset: Offset, canvasSize: Size, sweepAngles: List<Float>): Int? {
    val strokeWidth = canvasSize.minDimension * 0.28f
    val arcRadius = (canvasSize.minDimension - strokeWidth) / 2f
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val dx = tapOffset.x - center.x
    val dy = tapOffset.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance < arcRadius - strokeWidth / 2f || distance > arcRadius + strokeWidth / 2f) return null

    val angleFromStart = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f).let { if (it < 0f) it + 360f else it }
    var cumulative = 0f
    sweepAngles.forEachIndexed { index, sweep ->
        if (angleFromStart in cumulative..(cumulative + sweep)) return index
        cumulative += sweep
    }
    return null
}

@Composable
private fun PortfolioMixCard(slices: List<TypeValueSlice>, onClick: () -> Unit, onSliceClick: (SavedInvestmentType) -> Unit) {
    val total = slices.sumOf { it.value }
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(slices) { animationStarted = true }
    val progress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "portfolioMixProgress"
    )
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.investments_analysis_portfolio_mix_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Canvas(
                    modifier = Modifier
                        .size(96.dp)
                        .pointerInput(slices, total) {
                            detectTapGestures { tapOffset ->
                                val sweeps = slices.map { (it.value / total * 360.0).toFloat() }
                                val tapped = hitTestSlice(tapOffset, size.toSize(), sweeps)
                                selectedIndex = if (tapped != null && tapped == selectedIndex) null else tapped
                            }
                        }
                ) {
                    val strokeWidth = size.minDimension * 0.28f
                    var startAngle = -90f
                    slices.forEachIndexed { index, slice ->
                        val sweep = (slice.value / total * 360.0).toFloat() * progress
                        drawArc(
                            color = investmentTypeChartColor(slice.type),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = if (index == selectedIndex) strokeWidth * 1.25f else strokeWidth),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                        startAngle += sweep
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    slices.forEachIndexed { index, slice ->
                        val percent = if (total > 0) (slice.value / total * 100).toInt() else 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { selectedIndex = if (index == selectedIndex) null else index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(investmentTypeChartColor(slice.type), CircleShape)
                            )
                            Text(
                                text = "${investmentTypeLabel(slice.type)} · $percent%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            selectedIndex?.let { index ->
                val slice = slices.getOrNull(index) ?: return@let
                Text(
                    text = "${investmentTypeLabel(slice.type)}: ${currencyFormat.format(slice.value)} →",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSliceClick(slice.type) }
                )
            }
        }
    }
}

/** Breaks down SIP/Lumpsum entries by what they're actually invested in — Mutual Fund, Stocks,
 * Crypto, Gold, Other. A separate chart from [PortfolioMixCard] since it's a different taxonomy
 * (only two of the nine saved-investment types carry this dimension). */
@Composable
private fun AssetClassMixCard(slices: List<AssetValueSlice>, onClick: () -> Unit, onSliceClick: (AssetCategory) -> Unit) {
    val total = slices.sumOf { it.value }
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(slices) { animationStarted = true }
    val progress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "assetClassMixProgress"
    )
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.investments_analysis_asset_mix_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Canvas(
                    modifier = Modifier
                        .size(96.dp)
                        .pointerInput(slices, total) {
                            detectTapGestures { tapOffset ->
                                val sweeps = slices.map { (it.value / total * 360.0).toFloat() }
                                val tapped = hitTestSlice(tapOffset, size.toSize(), sweeps)
                                selectedIndex = if (tapped != null && tapped == selectedIndex) null else tapped
                            }
                        }
                ) {
                    val strokeWidth = size.minDimension * 0.28f
                    var startAngle = -90f
                    slices.forEachIndexed { index, slice ->
                        val sweep = (slice.value / total * 360.0).toFloat() * progress
                        drawArc(
                            color = assetCategoryChartColor(slice.category),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = if (index == selectedIndex) strokeWidth * 1.25f else strokeWidth),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                        startAngle += sweep
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    slices.forEachIndexed { index, slice ->
                        val percent = if (total > 0) (slice.value / total * 100).toInt() else 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { selectedIndex = if (index == selectedIndex) null else index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(assetCategoryChartColor(slice.category), CircleShape)
                            )
                            Text(
                                text = "${assetCategoryLabel(slice.category)} · $percent%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            selectedIndex?.let { index ->
                val slice = slices.getOrNull(index) ?: return@let
                Text(
                    text = "${assetCategoryLabel(slice.category)}: ${currencyFormat.format(slice.value)} →",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSliceClick(slice.category) }
                )
            }
        }
    }
}

@Composable
private fun UpcomingMaturitiesCard(maturities: List<UpcomingMaturity>, onClick: () -> Unit) {
    // Capped here — Home is a quick-glance summary, not the full list (that's the
    // Investments tab's job). Someone with many FDs/RDs maturing within the 90-day
    // window could otherwise make this card push everything else far down the scroll.
    val previewCount = 3
    val preview = maturities.take(previewCount)
    val remaining = maturities.size - preview.size

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.investments_analysis_upcoming_maturities_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val now = remember { System.currentTimeMillis() }
            preview.forEach { maturity ->
                val daysLeft = TimeUnit.MILLISECONDS.toDays(maturity.maturityDateMillis - now)
                val displayName = "${maturity.item.customName} (${investmentTypeLabel(maturity.item.type)})"
                val text = if (daysLeft <= 0) {
                    stringResource(R.string.investments_analysis_maturity_row_today, displayName)
                } else {
                    stringResource(
                        R.string.investments_analysis_maturity_row,
                        displayName,
                        daysLeft.toInt(),
                        formatDate(maturity.maturityDateMillis)
                    )
                }
                Text(text = text, style = MaterialTheme.typography.bodySmall)
            }
            if (remaining > 0) {
                Text(
                    text = stringResource(R.string.dashboard_maturities_more, remaining),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    )
}
