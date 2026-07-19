package com.financeplanner.app.domain.model

/** Light/Dark/System toggle — orthogonal to the color preset chosen below. */
enum class ThemeMode { LIGHT, DARK, SYSTEM_DEFAULT }

/**
 * Curated color presets. Deliberately a fixed set (not full Material You
 * dynamic color) per the requirement for distinct, curated theme options
 * plus a "randomize" action that picks one of these at random.
 */
enum class ThemePreset(val displayNameKey: String) {
    EMERALD("theme_preset_emerald"),
    OCEAN("theme_preset_ocean"),
    SUNSET("theme_preset_sunset"),
    ORCHID("theme_preset_orchid"),
    SLATE("theme_preset_slate");

    companion object {
        fun random(exclude: ThemePreset? = null): ThemePreset {
            val options = entries.filter { it != exclude }.ifEmpty { entries.toList() }
            return options.random()
        }
    }
}

/** Supported app languages, launch set per requirements: en, hi, mr, ta, te. */
enum class AppLanguage(val localeTag: String, val displayNameKey: String) {
    ENGLISH("en", "language_english"),
    HINDI("hi", "language_hindi"),
    MARATHI("mr", "language_marathi"),
    TAMIL("ta", "language_tamil"),
    TELUGU("te", "language_telugu")
}

/** Combined app-wide display preferences, persisted locally in Phase 1 (DataStore). */
data class AppDisplayPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    val themePreset: ThemePreset = ThemePreset.EMERALD,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hasSeenAppTour: Boolean = false
)
