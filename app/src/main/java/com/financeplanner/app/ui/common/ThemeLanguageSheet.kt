package com.financeplanner.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.AppDisplayPreferences
import com.financeplanner.app.domain.model.AppLanguage
import com.financeplanner.app.domain.model.ThemeMode
import com.financeplanner.app.domain.model.ThemePreset

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
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_mode),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
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

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_color),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                OutlinedButton(onClick = onRandomizeTheme) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.settings_randomize_theme))
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemePreset.entries) { preset ->
                    FilterChip(
                        selected = preferences.themePreset == preset,
                        onClick = { onThemePresetChange(preset) },
                        label = { Text(presetLabel(preset)) }
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_language),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
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
private fun presetLabel(preset: ThemePreset): String = when (preset) {
    ThemePreset.EMERALD -> stringResource(R.string.theme_preset_emerald)
    ThemePreset.OCEAN -> stringResource(R.string.theme_preset_ocean)
    ThemePreset.SUNSET -> stringResource(R.string.theme_preset_sunset)
    ThemePreset.ORCHID -> stringResource(R.string.theme_preset_orchid)
    ThemePreset.SLATE -> stringResource(R.string.theme_preset_slate)
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.HINDI -> stringResource(R.string.language_hindi)
    AppLanguage.MARATHI -> stringResource(R.string.language_marathi)
    AppLanguage.TAMIL -> stringResource(R.string.language_tamil)
    AppLanguage.TELUGU -> stringResource(R.string.language_telugu)
}
