package com.nexory.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.data.websocket.ChatWebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api:          NexoryApi,
    private val tokenManager: TokenManager,
    private val wsManager:    ChatWebSocketManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    // POST /auth/login
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Заполни все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.login(mapOf("email" to email, "password" to password))
                // Сохраняем токены и userId в DataStore
                tokenManager.saveTokens(
                    accessToken  = response.accessToken,
                    refreshToken = response.refreshToken,
                    userId       = response.user.id,
                )
                // Подключаем WebSocket сразу после логина
                wsManager.connect()
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("401") == true -> "Неверный email или пароль"
                    e.message?.contains("network") == true -> "Нет подключения к интернету"
                    else -> "Ошибка: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    // POST /auth/register
    fun register(username: String, email: String, password: String, phone: String?) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Заполни все обязательные поля") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(error = "Пароль должен быть не менее 8 символов") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val body = buildMap<String, String> {
                    put("username", username)
                    put("email", email)
                    put("password", password)
                    if (!phone.isNullOrBlank()) put("phone", phone)
                }
                val response = api.register(body)
                tokenManager.saveTokens(
                    response.accessToken, response.refreshToken, response.user.id
                )
                wsManager.connect()
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("409") == true -> "Email или имя пользователя уже заняты"
                    else -> "Ошибка регистрации: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }
}