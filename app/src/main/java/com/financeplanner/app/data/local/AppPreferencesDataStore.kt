package com.financeplanner.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.financeplanner.app.domain.model.AppDisplayPreferences
import com.financeplanner.app.domain.model.AppLanguage
import com.financeplanner.app.domain.model.InvestmentsViewMode
import com.financeplanner.app.domain.model.ThemeMode
import com.financeplanner.app.domain.model.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Local-only preferences store for Phase 1 (see technical design doc,
 * Section 1: DataStore for settings/theme/language prefs, no login needed).
 * In Phase 2 this stays as-is for guest users; a logged-in user's prefs
 * additionally sync to the backend via PUT /api/v1/preferences.
 */
class AppPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val LANGUAGE = stringPreferencesKey("language")
        val HAS_SEEN_APP_TOUR = booleanPreferencesKey("has_seen_app_tour")
        val DEFAULT_INFLATION_PERCENT = doublePreferencesKey("default_inflation_percent")
        val DEFAULT_EXPECTED_RETURN_PERCENT = doublePreferencesKey("default_expected_return_percent")
        val HAS_SET_DEFAULT_RATES = booleanPreferencesKey("has_set_default_rates")
        val LOCAL_DATA_POPUP_SHOWN_COUNT = intPreferencesKey("local_data_popup_shown_count")
        val RANDOMIZE_ON_LAUNCH = booleanPreferencesKey("randomize_on_launch")
        val USER_NAME = stringPreferencesKey("user_name")
        val HAS_ASKED_USER_NAME = booleanPreferencesKey("has_asked_user_name")
        val GREEN_DEFAULT_MIGRATION_APPLIED = booleanPreferencesKey("green_default_migration_applied")
        val INVESTMENTS_VIEW_MODE = stringPreferencesKey("investments_view_mode")
        val MATURITY_REMINDERS_ENABLED = booleanPreferencesKey("maturity_reminders_enabled")
        val HAS_REQUESTED_NOTIFICATION_PERMISSION = booleanPreferencesKey("has_requested_notification_permission")
        val HAS_SHOWN_MATURITY_REMINDER_INTRO = booleanPreferencesKey("has_shown_maturity_reminder_intro")
    }

    val preferencesFlow: Flow<AppDisplayPreferences> = dataStore.data.map { prefs ->
        val defaults = AppDisplayPreferences()
        AppDisplayPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM_DEFAULT,
            // valueOf throws (caught by runCatching) for a preset that no longer exists —
            // e.g. VIBRANT, removed after user feedback — which naturally falls back to
            // the default below instead of crashing.
            themePreset = prefs[Keys.THEME_PRESET]?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() }
                ?: ThemePreset.GREEN,
            language = prefs[Keys.LANGUAGE]?.let { code -> AppLanguage.entries.find { it.localeTag == code } }
                ?: AppLanguage.ENGLISH,
            hasSeenAppTour = prefs[Keys.HAS_SEEN_APP_TOUR] ?: false,
            defaultInflationPercent = prefs[Keys.DEFAULT_INFLATION_PERCENT] ?: defaults.defaultInflationPercent,
            defaultExpectedReturnPercent = prefs[Keys.DEFAULT_EXPECTED_RETURN_PERCENT]
                ?: defaults.defaultExpectedReturnPercent,
            hasSetDefaultRates = prefs[Keys.HAS_SET_DEFAULT_RATES] ?: false,
            localDataPopupShownCount = prefs[Keys.LOCAL_DATA_POPUP_SHOWN_COUNT] ?: 0,
            randomizeOnLaunch = prefs[Keys.RANDOMIZE_ON_LAUNCH] ?: false,
            userName = prefs[Keys.USER_NAME],
            hasAskedUserName = prefs[Keys.HAS_ASKED_USER_NAME] ?: false,
            investmentsViewMode = prefs[Keys.INVESTMENTS_VIEW_MODE]?.let { runCatching { InvestmentsViewMode.valueOf(it) }.getOrNull() }
                ?: InvestmentsViewMode.LIST,
            maturityRemindersEnabled = prefs[Keys.MATURITY_REMINDERS_ENABLED] ?: false,
            hasRequestedNotificationPermission = prefs[Keys.HAS_REQUESTED_NOTIFICATION_PERMISSION] ?: false,
            hasShownMaturityReminderIntro = prefs[Keys.HAS_SHOWN_MATURITY_REMINDER_INTRO] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        dataStore.edit { it[Keys.THEME_PRESET] = preset.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.localeTag }
    }

    suspend fun setHasSeenAppTour(seen: Boolean) {
        dataStore.edit { it[Keys.HAS_SEEN_APP_TOUR] = seen }
    }

    suspend fun setDefaultInflationPercent(value: Double) {
        dataStore.edit { it[Keys.DEFAULT_INFLATION_PERCENT] = value }
    }

    suspend fun setDefaultExpectedReturnPercent(value: Double) {
        dataStore.edit { it[Keys.DEFAULT_EXPECTED_RETURN_PERCENT] = value }
    }

    suspend fun setHasSetDefaultRates(value: Boolean) {
        dataStore.edit { it[Keys.HAS_SET_DEFAULT_RATES] = value }
    }

    suspend fun setRandomizeOnLaunch(value: Boolean) {
        dataStore.edit { it[Keys.RANDOMIZE_ON_LAUNCH] = value }
    }

    suspend fun incrementLocalDataPopupShownCount() {
        dataStore.edit { prefs ->
            val current = prefs[Keys.LOCAL_DATA_POPUP_SHOWN_COUNT] ?: 0
            prefs[Keys.LOCAL_DATA_POPUP_SHOWN_COUNT] = current + 1
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun setHasAskedUserName(value: Boolean) {
        dataStore.edit { it[Keys.HAS_ASKED_USER_NAME] = value }
    }

    suspend fun setInvestmentsViewMode(mode: InvestmentsViewMode) {
        dataStore.edit { it[Keys.INVESTMENTS_VIEW_MODE] = mode.name }
    }

    suspend fun setMaturityRemindersEnabled(value: Boolean) {
        dataStore.edit { it[Keys.MATURITY_REMINDERS_ENABLED] = value }
    }

    suspend fun setHasRequestedNotificationPermission(value: Boolean) {
        dataStore.edit { it[Keys.HAS_REQUESTED_NOTIFICATION_PERMISSION] = value }
    }

    suspend fun setHasShownMaturityReminderIntro(value: Boolean) {
        dataStore.edit { it[Keys.HAS_SHOWN_MATURITY_REMINDER_INTRO] = value }
    }

    /**
     * One-time forced switch to the Green preset — done once for every install,
     * even one that already had a different preset explicitly chosen (Green
     * became the app's default after the fact, to match the logo). Idempotent:
     * the migration flag makes sure this only overrides the user's choice once,
     * never again on subsequent launches after they've had a chance to change it.
     */
    suspend fun applyGreenDefaultMigrationIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[Keys.GREEN_DEFAULT_MIGRATION_APPLIED] != true) {
                prefs[Keys.THEME_PRESET] = ThemePreset.GREEN.name
                prefs[Keys.GREEN_DEFAULT_MIGRATION_APPLIED] = true
            }
        }
    }
}
