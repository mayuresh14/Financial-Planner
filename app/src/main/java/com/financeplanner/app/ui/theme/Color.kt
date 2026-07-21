package com.financeplanner.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

// Picks black or white for text/icons placed on top of `color`, based on
// its luminance, so every derived role stays readable regardless of preset.
private fun onColorFor(color: Color): Color =
    if (color.luminance() > 0.45f) Color.Black else Color.White

// "Container" tones (used for the tinted card backgrounds, badges, etc.)
// are a wash of the seed color toward white (light theme) or black (dark
// theme) — this is what makes cards/badges actually track the selected
// preset instead of sitting on Material's default baseline purple.
private fun containerFor(color: Color, isDark: Boolean): Color =
    lerp(color, if (isDark) Color.Black else Color.White, if (isDark) 0.6f else 0.8f)

private fun neutralSurface(seed: Color, isDark: Boolean, fraction: Float): Color =
    lerp(seed, if (isDark) Color.Black else Color.White, fraction)

private fun buildColorScheme(seed: PresetSeed, isDark: Boolean): ColorScheme {
    // Dark theme swaps primary/secondary so the brighter of the two leads,
    // matching Material's dark-theme guidance of desaturated-but-visible primaries.
    val primary = if (isDark) seed.secondary else seed.primary
    val secondary = if (isDark) seed.primary else seed.secondary
    val tertiary = seed.tertiary

    val primaryContainer = containerFor(primary, isDark)
    val secondaryContainer = containerFor(secondary, isDark)
    val tertiaryContainer = containerFor(tertiary, isDark)

    val background = neutralSurface(seed.primary, isDark, if (isDark) 0.92f else 0.96f)
    val onBackground = onColorFor(background)
    val surfaceVariant = neutralSurface(seed.primary, isDark, if (isDark) 0.85f else 0.90f)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = onColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = onColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onColorFor(tertiaryContainer),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onColorFor(surfaceVariant)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = onColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = onColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onColorFor(tertiaryContainer),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onColorFor(surfaceVariant)
        )
    }
}

fun lightColorSchemeFor(preset: ThemePreset): ColorScheme =
    buildColorScheme(presetSeeds.getValue(preset), isDark = false)

fun darkColorSchemeFor(preset: ThemePreset): ColorScheme =
    buildColorScheme(presetSeeds.getValue(preset), isDark = true)
