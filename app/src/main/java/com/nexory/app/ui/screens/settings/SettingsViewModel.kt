package com.nexory.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.SettingsManager
import com.nexory.app.data.local.ThemeMode
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Настройки профиля, приходящие с сервера
data class ProfilePrefs(
    val notificationsEnabled: Boolean = true,
    val notifyMessages:       Boolean = true,
    val notifyFriendEvents:   Boolean = true,
    val notifyInterestEvents: Boolean = true,
    val profileVisibility:    String  = "friends",
)

data class SettingsUiState(
    val themeMode:            ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean   = true,
    val notifyMessages:       Boolean   = true,
    val notifyFriendEvents:   Boolean   = true,
    val notifyInterestEvents: Boolean   = true,
    val profileVisibility:    String    = "friends",
    val pinEnabled:           Boolean   = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsManager,
    private val api:      NexoryApi,
) : ViewModel() {

    private val _prefs = MutableStateFlow(ProfilePrefs())

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.themeMode, settings.pinEnabled, _prefs) { theme, pin, p ->
            SettingsUiState(
                themeMode = theme,
                notificationsEnabled = p.notificationsEnabled,
                notifyMessages = p.notifyMessages,
                notifyFriendEvents = p.notifyFriendEvents,
                notifyInterestEvents = p.notifyInterestEvents,
                profileVisibility = p.profileVisibility,
                pinEnabled = pin,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        viewModelScope.launch {
            try {
                val u = api.getMyProfile().user
                if (u != null) _prefs.value = ProfilePrefs(
                    notificationsEnabled = u.notificationsEnabled,
                    notifyMessages = u.notifyMessages,
                    notifyFriendEvents = u.notifyFriendEvents,
                    notifyInterestEvents = u.notifyInterestEvents,
                    profileVisibility = u.profileVisibility,
                )
            } catch (_: Exception) {}
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    private fun patch(field: String, value: Boolean, update: (ProfilePrefs) -> ProfilePrefs) {
        _prefs.update(update)
        viewModelScope.launch {
            try { api.updateProfile(mapOf(field to value.toString())) } catch (_: Exception) {}
        }
    }

    fun setNotificationsEnabled(v: Boolean) = patch("notifications_enabled", v) { it.copy(notificationsEnabled = v) }
    fun setNotifyMessages(v: Boolean)       = patch("notify_messages", v) { it.copy(notifyMessages = v) }
    fun setNotifyFriendEvents(v: Boolean)   = patch("notify_friend_events", v) { it.copy(notifyFriendEvents = v) }
    fun setNotifyInterestEvents(v: Boolean) = patch("notify_interest_events", v) { it.copy(notifyInterestEvents = v) }

    fun setVisibility(level: String) {
        _prefs.update { it.copy(profileVisibility = level) }
        viewModelScope.launch {
            try { api.updateProfile(mapOf("profile_visibility" to level)) } catch (_: Exception) {}
        }
    }

    fun setPin(pin: String) { viewModelScope.launch { settings.setPin(pin) } }
    fun disablePin() { viewModelScope.launch { settings.disablePin() } }
}
