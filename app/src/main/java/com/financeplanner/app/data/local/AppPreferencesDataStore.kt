package com.financeplanner.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.financeplanner.app.domain.model.AppDisplayPreferences
import com.financeplanner.app.domain.model.AppLanguage
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
    }

    val preferencesFlow: Flow<AppDisplayPreferences> = dataStore.data.map { prefs ->
        val defaults = AppDisplayPreferences()
        AppDisplayPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM_DEFAULT,
            themePreset = prefs[Keys.THEME_PRESET]?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() }
                ?: ThemePreset.EMERALD,
            language = prefs[Keys.LANGUAGE]?.let { code -> AppLanguage.entries.find { it.localeTag == code } }
                ?: AppLanguage.ENGLISH,
            hasSeenAppTour = prefs[Keys.HAS_SEEN_APP_TOUR] ?: false,
            defaultInflationPercent = prefs[Keys.DEFAULT_INFLATION_PERCENT] ?: defaults.defaultInflationPercent,
            defaultExpectedReturnPercent = prefs[Keys.DEFAULT_EXPECTED_RETURN_PERCENT]
                ?: defaults.defaultExpectedReturnPercent,
            hasSetDefaultRates = prefs[Keys.HAS_SET_DEFAULT_RATES] ?: false
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
}
