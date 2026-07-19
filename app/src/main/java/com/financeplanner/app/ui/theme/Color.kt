package com.financeplanner.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.financeplanner.app.domain.model.ThemePreset

// Each preset defines a seed/primary color; secondary/tertiary are derived
// to stay visually coherent without needing a full Material Theme Builder
// export for every preset. Values chosen for reasonable contrast in both
// light and dark surfaces.

private data class PresetSeed(val primary: Color, val secondary: Color, val tertiary: Color)

private val presetSeeds = mapOf(
    ThemePreset.EMERALD to PresetSeed(
        primary = Color(0xFF00695C),
        secondary = Color(0xFF4DB6AC),
        tertiary = Color(0xFFFFB74D)
    ),
    ThemePreset.OCEAN to PresetSeed(
        primary = Color(0xFF01579B),
        secondary = Color(0xFF4FC3F7),
        tertiary = Color(0xFFFFA726)
    ),
    ThemePreset.SUNSET to PresetSeed(
        primary = Color(0xFFD84315),
        secondary = Color(0xFFFFAB91),
        tertiary = Color(0xFF5C6BC0)
    ),
    ThemePreset.ORCHID to PresetSeed(
        primary = Color(0xFF6A1B9A),
        secondary = Color(0xFFBA68C8),
        tertiary = Color(0xFF66BB6A)
    ),
    ThemePreset.SLATE to PresetSeed(
        primary = Color(0xFF37474F),
        secondary = Color(0xFF90A4AE),
        tertiary = Color(0xFFFFCA28)
    )
)

fun lightColorSchemeFor(preset: ThemePreset): ColorScheme {
    val seed = presetSeeds.getValue(preset)
    return lightColorScheme(
        primary = seed.primary,
        secondary = seed.secondary,
        tertiary = seed.tertiary
    )
}

fun darkColorSchemeFor(preset: ThemePreset): ColorScheme {
    val seed = presetSeeds.getValue(preset)
    return darkColorScheme(
        primary = seed.secondary,
        secondary = seed.primary,
        tertiary = seed.tertiary
    )
}
