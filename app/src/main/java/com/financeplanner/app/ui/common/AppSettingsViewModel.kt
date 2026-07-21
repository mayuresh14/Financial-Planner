package com.financeplanner.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.analytics.AppAnalytics
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.AppDisplayPreferences
import com.financeplanner.app.domain.model.AppLanguage
import com.financeplanner.app.domain.model.ThemeMode
import com.financeplanner.app.domain.model.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared theme/language state, read by FinancePlannerTheme at the app root
 * and mutated from the settings sheet on any screen (see ThemeLanguageSheet).
 * Scope this ViewModel to the Activity (hiltViewModel(activity) or hoist it
 * once at the NavHost level) so every screen observes the same instance.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val preferencesDataStore: AppPreferencesDataStore,
    private val analytics: AppAnalytics
) : ViewModel() {

    // preferencesFlow reads DataStore asynchronously; stateIn must seed an
    // initial value before that first read completes. Using the "not seen"
    // default here would make any first-frame consumer (e.g. HomeScreen's
    // app-tour gate) briefly see hasSeenAppTour = false even for a returning
    // user. hasLoadedPreferences flips true only once the real stored value
    // has arrived, so such consumers can wait for it before acting.
    private val _hasLoadedPreferences = MutableStateFlow(false)
    val hasLoadedPreferences: StateFlow<Boolean> = _hasLoadedPreferences.asStateFlow()

    val preferences: StateFlow<AppDisplayPreferences> = preferencesDataStore.preferencesFlow
        .onEach { _hasLoadedPreferences.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppDisplayPreferences())

    fun setThemeMode(mode: ThemeMode) {
        analytics.logSettingChanged(SETTING_THEME_MODE, mode.name)
        viewModelScope.launch { preferencesDataStore.setThemeMode(mode) }
    }

    fun setThemePreset(preset: ThemePreset) {
        analytics.logSettingChanged(SETTING_THEME_PRESET, preset.name)
        viewModelScope.launch { preferencesDataStore.setThemePreset(preset) }
    }

    fun randomizeTheme() {
        viewModelScope.launch {
            val current = preferences.value.themePreset
            val randomized = ThemePreset.random(exclude = current)
            analytics.logSettingChanged(SETTING_THEME_PRESET, randomized.name)
            preferencesDataStore.setThemePreset(randomized)
        }
    }

    fun setLanguage(language: AppLanguage) {
        analytics.logSettingChanged(SETTING_LANGUAGE, language.name)
        viewModelScope.launch { preferencesDataStore.setLanguage(language) }
    }

    fun setHasSeenAppTour(seen: Boolean) {
        if (seen) analytics.logAppTourDismissed()
        viewModelScope.launch { preferencesDataStore.setHasSeenAppTour(seen) }
    }

    fun logAppTourShown() {
        analytics.logAppTourShown()
    }

    fun setDefaultInflationPercent(value: Double) {
        analytics.logSettingChanged(SETTING_DEFAULT_INFLATION, value.toString())
        viewModelScope.launch { preferencesDataStore.setDefaultInflationPercent(value) }
    }

    fun setDefaultExpectedReturnPercent(value: Double) {
        analytics.logSettingChanged(SETTING_DEFAULT_EXPECTED_RETURN, value.toString())
        viewModelScope.launch { preferencesDataStore.setDefaultExpectedReturnPercent(value) }
    }

    fun setHasSetDefaultRates(value: Boolean) {
        viewModelScope.launch { preferencesDataStore.setHasSetDefaultRates(value) }
    }

    private companion object {
        const val SETTING_THEME_MODE = "theme_mode"
        const val SETTING_THEME_PRESET = "theme_preset"
        const val SETTING_LANGUAGE = "language"
        const val SETTING_DEFAULT_INFLATION = "default_inflation_percent"
        const val SETTING_DEFAULT_EXPECTED_RETURN = "default_expected_return_percent"
    }
}
