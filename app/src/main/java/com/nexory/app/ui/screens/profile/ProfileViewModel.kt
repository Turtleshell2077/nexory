package com.nexory.app.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.MediaUploader
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.data.network.UserDto
import com.nexory.app.data.websocket.ChatWebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user:      UserDto? = null,
    val isLoading: Boolean  = false,
    val uploadingAvatar: Boolean = false,
    val error:     String?  = null,
    /** Профиль показан из локального кэша (нет сети). */
    val isFromCache: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api:          NexoryApi,
    private val tokenManager: TokenManager,
    private val wsManager:    ChatWebSocketManager,
    private val uploader:     MediaUploader,
    private val cache:        com.nexory.app.data.local.OfflineCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init { loadProfile() }

    // Загрузить новый аватар с галереи и сохранить
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingAvatar = true) }
            val url = uploader.upload(uri)
            if (url != null) {
                try {
                    val response = api.updateProfile(mapOf("avatar_url" to url))
                    _uiState.update { it.copy(user = response.user) }
                } catch (_: Exception) {}
            }
            _uiState.update { it.copy(uploadingAvatar = false) }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = api.getMyProfile()
                response.user?.let { cache.saveMyProfile(it) }
                _uiState.update { it.copy(user = response.user, isLoading = false, isFromCache = false, error = null) }
            } catch (e: Exception) {
                // Оффлайн: показываем последнюю сохранённую версию профиля вместо ошибки
                val cached = cache.loadMyProfile()
                if (cached != null) {
                    _uiState.update { it.copy(user = cached, isLoading = false, isFromCache = true, error = null) }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = com.nexory.app.data.network.ApiError.message(e))
                    }
                }
            }
        }
    }

    fun updateProfile(username: String?, bio: String?, avatarUrl: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val body = buildMap<String, String?> {
                    if (username != null) put("username", username)
                    if (bio      != null) put("bio", bio)
                    if (avatarUrl != null) put("avatar_url", avatarUrl)
                }
                val response = api.updateProfile(body)
                _uiState.update { it.copy(user = response.user, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val refreshToken = tokenManager.getRefreshToken()
            try {
                if (refreshToken != null) {
                    api.logout(mapOf("refreshToken" to refreshToken))
                }
            } catch (_: Exception) { /* игнорируем ошибку сети при логауте */ }
            wsManager.disconnect()
            // Локальный кэш чистим обязательно: иначе следующий пользователь на этом
            // устройстве увидел бы оффлайн чужую ленту, профиль и сообщения.
            cache.clearAll()
            tokenManager.clear()
        }
    }
}