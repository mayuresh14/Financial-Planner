package com.financeplanner.app.ui.common

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.AppDisplayPreferences
import com.financeplanner.app.domain.model.AppLanguage
import com.financeplanner.app.domain.model.ThemeMode
import com.financeplanner.app.domain.model.ThemePreset

/** Which reminder toggle is currently waiting on a POST_NOTIFICATIONS result. */
private enum class ReminderToggle { MATURITY, MONTHLY }

/**
 * Reusable settings sheet: theme mode (Light/Dark/System), curated theme
 * preset chips + "Randomize" action, and language selector. Every calculator
 * screen opens this via the same settings icon in its TopAppBar — this is
 * the shared pattern, not a per-screen reimplementation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeLanguageSheet(
    preferences: AppDisplayPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemePresetChange: (ThemePreset) -> Unit,
    onRandomizeTheme: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDefaultInflationChange: (Double) -> Unit,
    onDefaultExpectedReturnChange: (Double) -> Unit,
    onUserNameChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var nameText by remember(preferences.userName) { mutableStateOf(preferences.userName ?: "") }

    // Resolves to the same instance the host screen already holds (same NavBackStackEntry) —
    // fetched here rather than threaded through as a parameter so this one settings toggle
    // doesn't require touching the 20+ screens that already call ThemeLanguageSheet.
    val settingsViewModel: AppSettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    var showPermissionBlockedDialog by remember { mutableStateOf(false) }
    // Which toggle's own switch triggered the in-flight permission request — both reminder
    // types share the one POST_NOTIFICATIONS permission, but only the toggle actually being
    // switched on should flip when the result comes back, not both.
    var pendingPermissionToggle by remember { mutableStateOf<ReminderToggle?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsViewModel.setHasRequestedNotificationPermission(true)
        settingsViewModel.setHasShownMaturityReminderIntro(true)
        when (pendingPermissionToggle) {
            ReminderToggle.MATURITY -> settingsViewModel.setMaturityRemindersEnabled(granted)
            ReminderToggle.MONTHLY -> settingsViewModel.setMonthlyReminderEnabled(granted)
            null -> Unit
        }
        pendingPermissionToggle = null
    }

    if (showPermissionBlockedDialog) {
        NotificationPermissionBlockedDialog(
            onDismiss = { showPermissionBlockedDialog = false },
            onOpenSettings = {
                showPermissionBlockedDialog = false
                openAppNotificationSettings(context)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onDismiss()
            }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 0: Your Name — the dashboard greeting, editable here
            // since AskNameSheet only ever shows once on first launch.
            Text(
                text = stringResource(R.string.settings_your_name_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = nameText,
                onValueChange = {
                    nameText = it
                    onUserNameChange(it.trim())
                },
                label = { Text(stringResource(R.string.ask_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            // Section 1: Default Rates — surfaced first since these affect
            // every calculator's starting values, unlike the purely
            // cosmetic settings below.
            Text(
                text = stringResource(R.string.settings_default_rates_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            DefaultRateField(
                label = stringResource(R.string.settings_default_expected_return),
                value = preferences.defaultExpectedReturnPercent,
                onValueChange = onDefaultExpectedReturnChange
            )
            DefaultRateField(
                label = stringResource(R.string.settings_default_inflation),
                value = preferences.defaultInflationPercent,
                onValueChange = onDefaultInflationChange
            )

            HorizontalDivider()

            // Section 2: Reminders — a local notification for FDs/RDs nearing maturity, 7 days
            // out and on the day itself. Android 13+ needs POST_NOTIFICATIONS granted first.
            // Surfaced before Appearance since it's a functional setting, not a cosmetic one.
            Text(
                text = stringResource(R.string.settings_reminders_section_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_maturity_reminders_title),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_maturity_reminders_description),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.maturityRemindersEnabled,
                    onCheckedChange = { turningOn ->
                        if (!turningOn) {
                            settingsViewModel.setMaturityRemindersEnabled(false)
                        } else {
                            // The switch only actually turns on once permission is confirmed —
                            // CAN_PROMPT asks (and the launcher callback settles it either way),
                            // BLOCKED can't ask again, so guide to system Settings instead.
                            when (notificationPermissionState(context, preferences.hasRequestedNotificationPermission)) {
                                NotificationPermissionState.GRANTED -> settingsViewModel.setMaturityRemindersEnabled(true)
                                NotificationPermissionState.CAN_PROMPT -> {
                                    pendingPermissionToggle = ReminderToggle.MATURITY
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                NotificationPermissionState.BLOCKED -> showPermissionBlockedDialog = true
                            }
                        }
                    }
                )
            }
            // Re-evaluated fresh each time this sheet is opened (it's torn down on dismiss), so
            // this stays accurate after the user grants the permission from system Settings and
            // reopens the sheet — no lifecycle observer needed for that case. In practice this
            // should be rare/transient since MainActivity's launch-time check already turns the
            // setting off the moment it detects a revoked-and-blocked permission.
            val notificationsBlocked = (preferences.maturityRemindersEnabled || preferences.monthlyReminderEnabled) &&
                notificationPermissionState(context, preferences.hasRequestedNotificationPermission) != NotificationPermissionState.GRANTED
            if (notificationsBlocked) {
                Text(
                    text = stringResource(R.string.settings_maturity_reminders_blocked_hint),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable { openAppNotificationSettings(context) }
                )
            }

            HorizontalDivider()

            // Section 3: Appearance — theme mode, color, and language, all
            // purely cosmetic/UX preferences grouped under one umbrella.
            Text(
                text = stringResource(R.string.settings_appearance_section_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.settings_theme_mode),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    ThemeMode.LIGHT to R.string.theme_mode_light,
                    ThemeMode.DARK to R.string.theme_mode_dark,
                    ThemeMode.SYSTEM_DEFAULT to R.string.theme_mode_system
                )
                modes.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = preferences.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_theme_color),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            // Randomize chip temporarily hidden (not removed — flip this back
            // to true to re-enable). Underlying toggle/auto-pick-on-launch
            // logic in AppSettingsViewModel is untouched.
            val isRandomizeChipEnabled = false
            if (isRandomizeChipEnabled && preferences.randomizeOnLaunch) {
                Text(
                    text = stringResource(R.string.settings_randomize_theme_active_hint),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemePreset.entries) { preset ->
                    FilterChip(
                        selected = !preferences.randomizeOnLaunch && preferences.themePreset == preset,
                        onClick = { onThemePresetChange(preset) },
                        label = { Text(presetLabel(preset)) }
                    )
                }
                if (isRandomizeChipEnabled) {
                    item {
                        FilterChip(
                            selected = preferences.randomizeOnLaunch,
                            onClick = onRandomizeTheme,
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            label = { Text(stringResource(R.string.settings_randomize_theme)) }
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_language),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppLanguage.entries) { language ->
                    FilterChip(
                        selected = preferences.language == language,
                        onClick = { onLanguageChange(language) },
                        label = { Text(languageLabel(language)) }
                    )
                }
            }

        }
    }
}

@Composable
private fun DefaultRateField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new.filter { it.isDigit() || it == '.' }
            text.toDoubleOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun presetLabel(preset: ThemePreset): String = when (preset) {
    ThemePreset.GREEN -> stringResource(R.string.theme_preset_green)
    ThemePreset.OCEAN -> stringResource(R.string.theme_preset_ocean)
    ThemePreset.SUNSET -> stringResource(R.string.theme_preset_sunset)
    ThemePreset.ORCHID -> stringResource(R.string.theme_preset_orchid)
    ThemePreset.CRIMSON -> stringResource(R.string.theme_preset_crimson)
    ThemePreset.AMBER -> stringResource(R.string.theme_preset_amber)
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.HINDI -> stringResource(R.string.language_hindi)
    AppLanguage.MARATHI -> stringResource(R.string.language_marathi)
    AppLanguage.TAMIL -> stringResource(R.string.language_tamil)
    AppLanguage.TELUGU -> stringResource(R.string.language_telugu)
}
