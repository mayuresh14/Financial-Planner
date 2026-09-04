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
    // Matches the app's own logo (ic_launcher_background = #2E7D32) — the default preset,
    // so a fresh install's theme actually agrees with the icon on the home screen.
    ThemePreset.GREEN to PresetSeed(
        primary = Color(0xFF2E7D32),
        secondary = Color(0xFF66BB6A),
        tertiary = Color(0xFFFFC107)
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
    // Bold hot-pink/red pairing — punchier than the old muted Emerald/Slate options.
    ThemePreset.CRIMSON to PresetSeed(
        primary = Color(0xFFD50032),
        secondary = Color(0xFFFF4081),
        tertiary = Color(0xFF7C4DFF)
    ),
    // Gold/amber preset — a deep amber primary keeps contrast reasonable for text/icons
    // on top of it (a pale pure-yellow would fail there), with a warm honey-gold
    // secondary and a deep teal tertiary as the complementary accent.
    ThemePreset.AMBER to PresetSeed(
        primary = Color(0xFFF57F17),
        secondary = Color(0xFFFFB300),
        tertiary = Color(0xFF00695C)
    )
)

/**
 * Picks black or white for text/icons placed on top of [color] by actually comparing
 * WCAG-style contrast ratios against both, rather than a flat luminance cutoff — a fixed
 * cutoff (e.g. "luminance > 0.45 -> black") is a poor proxy once containers get more
 * saturated (see containerFor/neutralSurface below): a fully-saturated mid-luminance hue
 * can clear that cutoff yet still read as too dark for black text to sit on comfortably.
 */
private fun onColorFor(color: Color): Color {
    val luminance = color.luminance()
    val contrastWithBlack = (luminance + 0.05f) / 0.05f
    val contrastWithWhite = 1.05f / (luminance + 0.05f)
    return if (contrastWithBlack >= contrastWithWhite) Color.Black else Color.White
}

// "Container" tones (used for the tinted card backgrounds, badges, etc.)
// are a wash of the seed color toward white (light theme) or black (dark
// theme) — this is what makes cards/badges actually track the selected
// preset instead of sitting on Material's default baseline purple. Kept
// fairly light on the wash so the preset's actual hue stays visible instead
// of reading as generic gray. Dark mode's wash is lighter than light mode's
// (0.32 vs 0.72): blending a cool hue (blue/purple) toward black desaturates
// it much faster to the eye than blending a warm hue does at the same
// fraction, which was making Ocean/Orchid nearly indistinguishable from the
// neutral dark-gray chrome around them — warm presets (Sunset/Crimson/Amber)
// stayed readable at a much higher wash.
private fun containerFor(color: Color, isDark: Boolean): Color =
    lerp(color, if (isDark) Color.Black else Color.White, if (isDark) 0.32f else 0.72f)

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

    // The screen canvas itself is plain white (light) / near-black (dark) — no preset
    // tint at all. primary/secondary/tertiary and their containers carry the theme's
    // actual color; everything structural below is themed in light mode but, in dark
    // mode, deliberately NOT seed-tinted — washing a saturated hue down toward black
    // repeatedly (across surfaceVariant, 5 surfaceContainer steps, outline, etc.) kept
    // landing on low-contrast, muddy results that read as "unusable." Dark mode instead
    // uses a standard set of neutral Material dark-elevation grays for structure, with
    // the preset color still fully present on primary/secondary/tertiary, their
    // containers, and buttons — which is where a dark UI's accent color should live.
    val background = if (isDark) Color(0xFF121212) else Color.White
    val onBackground = onColorFor(background)

    val surfaceVariant = if (isDark) Color(0xFF2A2A2A) else neutralSurface(seed.primary, isDark, 0.80f)

    // The "surface container" family backs Material3's NavigationBar, ModalBottomSheet,
    // and AlertDialog by default. Left unset, they fall back to Compose's baseline
    // (purple-tinted) scheme regardless of the selected preset — this is what was making
    // the bottom nav, popups, and sheets ignore the app's theme entirely in light mode.
    val surfaceContainerLowest = background
    val surfaceContainerLow = if (isDark) Color(0xFF1B1B1B) else neutralSurface(seed.primary, isDark, 0.95f)
    val surfaceContainer = surfaceVariant
    val surfaceContainerHigh = if (isDark) Color(0xFF2F2F2F) else neutralSurface(seed.primary, isDark, 0.68f)
    val surfaceContainerHighest = if (isDark) Color(0xFF3A3A3A) else containerFor(primary, isDark)

    val outline = if (isDark) Color(0xFF8A8A8A) else neutralSurface(seed.primary, isDark, 0.50f)
    val outlineVariant = if (isDark) Color(0xFF454545) else neutralSurface(seed.primary, isDark, 0.82f)
    val inverseSurface = if (isDark) Color(0xFFF2F2F2) else Color(0xFF1C1C1C)
    val inverseOnSurface = onColorFor(inverseSurface)

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
            onSurfaceVariant = onColorFor(surfaceVariant),
            surfaceTint = primary,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface
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
            onSurfaceVariant = onColorFor(surfaceVariant),
            surfaceTint = primary,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface
        )
    }
}

fun lightColorSchemeFor(preset: ThemePreset): ColorScheme =
    buildColorScheme(presetSeeds.getValue(preset), isDark = false)

fun darkColorSchemeFor(preset: ThemePreset): ColorScheme =
    buildColorScheme(presetSeeds.getValue(preset), isDark = true)
