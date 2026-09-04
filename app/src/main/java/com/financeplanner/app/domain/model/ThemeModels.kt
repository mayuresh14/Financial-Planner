package com.financeplanner.app.domain.model

/** Light/Dark/System toggle — orthogonal to the color preset chosen below. */
enum class ThemeMode { LIGHT, DARK, SYSTEM_DEFAULT }

/**
 * Curated color presets. Deliberately a fixed set (not full Material You
 * dynamic color) per the requirement for distinct, curated theme options
 * plus a "randomize" action that picks one of these at random.
 */
enum class ThemePreset(val displayNameKey: String) {
    GREEN("theme_preset_green"),
    OCEAN("theme_preset_ocean"),
    SUNSET("theme_preset_sunset"),
    ORCHID("theme_preset_orchid"),
    CRIMSON("theme_preset_crimson"),
    AMBER("theme_preset_amber");

    companion object {
        fun random(exclude: ThemePreset? = null): ThemePreset {
            val options = entries.filter { it != exclude }.ifEmpty { entries.toList() }
            return options.random()
        }
    }
}

/** Investments tab layout: the plain filterable list, or a per-type summary that drills into it. */
enum class InvestmentsViewMode { LIST, SUMMARY }

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
    val themePreset: ThemePreset = ThemePreset.GREEN,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hasSeenAppTour: Boolean = false,
    val defaultInflationPercent: Double = 5.0,
    val defaultExpectedReturnPercent: Double = 12.0,
    val hasSetDefaultRates: Boolean = false,
    val localDataPopupShownCount: Int = 0,
    val randomizeOnLaunch: Boolean = false,
    val userName: String? = null,
    val hasAskedUserName: Boolean = false,
    val investmentsViewMode: InvestmentsViewMode = InvestmentsViewMode.LIST,
    val maturityRemindersEnabled: Boolean = false,
    /** True once the OS POST_NOTIFICATIONS prompt has actually been shown at least once —
     * distinct from [hasShownMaturityReminderIntro] below, and used to tell "never asked"
     * from "permanently denied" apart via shouldShowRequestPermissionRationale. */
    val hasRequestedNotificationPermission: Boolean = false,
    /** True once the one-time "enable maturity reminders?" launch dialog has been shown and
     * answered (Turn On or Not now) — gates that dialog to first launch only. */
    val hasShownMaturityReminderIntro: Boolean = false
)
