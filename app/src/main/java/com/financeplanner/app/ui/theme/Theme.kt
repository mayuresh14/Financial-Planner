package com.financeplanner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.financeplanner.app.domain.model.ThemeMode
import com.financeplanner.app.domain.model.ThemePreset

/**
 * Root theme composable. Wraps the whole app (see MainActivity) so a theme
 * change from the settings sheet recomposes everything below it reactively —
 * no activity restart needed.
 */
@Composable
fun FinancePlannerTheme(
    themeMode: ThemeMode,
    themePreset: ThemePreset,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) {
        darkColorSchemeFor(themePreset)
    } else {
        lightColorSchemeFor(themePreset)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinancePlannerTypography,
        content = content
    )
}
