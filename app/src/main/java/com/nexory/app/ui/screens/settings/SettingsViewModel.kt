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
    private val tokenManager: com.nexory.app.data.local.TokenManager,
    private val cache:    com.nexory.app.data.local.OfflineCache,
) : ViewModel() {

    private val _prefs = MutableStateFlow(ProfilePrefs())

    // Состояние удаления аккаунта
    private val _deleteState = MutableStateFlow(DeleteAccountState())
    val deleteState = _deleteState.asStateFlow()

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

    /**
     * Удаление аккаунта и всех данных (требование Google Play).
     * После успешного удаления чистим локальные токены и настройки — приложение
     * само уйдёт на экран входа, т.к. isLoggedIn станет false.
     */
    fun deleteAccount(password: String) {
        if (password.isBlank()) {
            _deleteState.value = DeleteAccountState(error = "Введите пароль")
            return
        }
        viewModelScope.launch {
            _deleteState.value = DeleteAccountState(isLoading = true)
            try {
                api.deleteAccount(mapOf("password" to password))
                // Полностью убираем локальные следы: кэш данных, PIN и токены.
                // Согласие с документами оставляем — оно относится к устройству,
                // а не к аккаунту, и повторно спрашивать его незачем.
                cache.clearAll()
                settings.disablePin()
                tokenManager.clear()
                _deleteState.value = DeleteAccountState(isDeleted = true)
            } catch (e: Exception) {
                _deleteState.value = DeleteAccountState(
                    error = com.nexory.app.data.network.ApiError.message(e)
                )
            }
        }
    }

    fun clearDeleteError() { _deleteState.update { it.copy(error = null) } }
}

data class DeleteAccountState(
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val error:     String? = null,
)
