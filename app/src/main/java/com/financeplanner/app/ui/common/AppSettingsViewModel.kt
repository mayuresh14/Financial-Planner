package com.financeplanner.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financeplanner.app.data.local.AppPreferencesDataStore
import com.financeplanner.app.domain.model.AppDisplayPreferences
import com.financeplanner.app.domain.model.AppLanguage
import com.financeplanner.app.domain.model.ThemeMode
import com.financeplanner.app.domain.model.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val preferencesDataStore: AppPreferencesDataStore
) : ViewModel() {

    val preferences: StateFlow<AppDisplayPreferences> = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppDisplayPreferences())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesDataStore.setThemeMode(mode) }
    }

    fun setThemePreset(preset: ThemePreset) {
        viewModelScope.launch { preferencesDataStore.setThemePreset(preset) }
    }

    fun randomizeTheme() {
        viewModelScope.launch {
            val current = preferences.value.themePreset
            preferencesDataStore.setThemePreset(ThemePreset.random(exclude = current))
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesDataStore.setLanguage(language) }
    }

    fun setHasSeenAppTour(seen: Boolean) {
        viewModelScope.launch { preferencesDataStore.setHasSeenAppTour(seen) }
    }
}
