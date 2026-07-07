package com.nexory.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Отдельное хранилище для пользовательских настроек приложения
private val Context.settingsStore: DataStore<Preferences>
        by preferencesDataStore(name = "nexory_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_THEME         = stringPreferencesKey("theme_mode")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_ONBOARDING    = booleanPreferencesKey("onboarding_done")
    }

    val onboardingDone: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[KEY_ONBOARDING] ?: false
    }

    suspend fun setOnboardingDone() {
        context.settingsStore.edit { it[KEY_ONBOARDING] = true }
    }

    val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "light" -> ThemeMode.LIGHT
            "dark"  -> ThemeMode.DARK
            else    -> ThemeMode.SYSTEM
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[KEY_THEME] = mode.name.lowercase() }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }
}
