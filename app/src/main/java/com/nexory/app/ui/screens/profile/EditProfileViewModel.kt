package com.nexory.app.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.network.MediaUploader
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.data.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val user:      UserDto? = null,
    val isLoading: Boolean  = false,
    val isSaved:   Boolean  = false,
    val error:     String?  = null,
    // Смена пароля
    val passwordMessage: String? = null,
    val passwordError:   String? = null,
    val passwordLoading: Boolean = false,
    // Сброс через почту
    val resetCodeSent:   Boolean = false,
    val resetMessage:    String? = null,
    val resetError:      String? = null,
    val resetLoading:    Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val api: NexoryApi,
    private val uploader: MediaUploader,
) : ViewModel() {

    // Загружает фото на сервер, возвращает постоянный URL
    suspend fun uploadImage(uri: Uri): String? = uploader.upload(uri)

    private val _state = MutableStateFlow(EditProfileUiState())
    val state = _state.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = api.getMyProfile()
                _state.update { it.copy(user = response.user, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun save(
        username:    String,
        displayName: String,
        bio:         String,
        avatarUrl:   String,
        age:         Int?,
        city:        String,
        interests:   String,   // увлечения через запятую — храним в колонке sports
        phone:       String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val body = buildMap<String, String?> {
                    if (username.isNotBlank())    put("username",     username)
                    if (displayName.isNotBlank()) put("display_name", displayName)
                    put("bio",        bio)            // допускаем очистку
                    if (avatarUrl.isNotBlank())   put("avatar_url",   avatarUrl)
                    if (age != null)              put("age",          age.toString())
                    put("city",       city)
                    put("sports",     interests)
                    put("phone",      phone)
                }
                val response = api.updateProfile(body)
                _state.update { it.copy(user = response.user, isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("409") == true -> "Никнейм уже занят"
                    else -> "Ошибка сохранения: ${e.message}"
                }
                _state.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    // Запросить код сброса на email пользователя
    fun requestPasswordReset() {
        val email = _state.value.user?.email ?: return
        viewModelScope.launch {
            _state.update { it.copy(resetLoading = true, resetError = null, resetMessage = null) }
            try {
                api.requestPasswordReset(mapOf("email" to email))
                _state.update { it.copy(resetLoading = false, resetCodeSent = true, resetMessage = "Код отправлен на $email") }
            } catch (e: Exception) {
                _state.update { it.copy(resetLoading = false, resetError = "Не удалось отправить код") }
            }
        }
    }

    // Сменить пароль по коду из письма
    fun resetPasswordWithCode(code: String, newPassword: String) {
        val email = _state.value.user?.email ?: return
        if (newPassword.length < 8) {
            _state.update { it.copy(resetError = "Новый пароль должен быть не менее 8 символов") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(resetLoading = true, resetError = null) }
            try {
                api.resetPassword(mapOf("email" to email, "code" to code, "newPassword" to newPassword))
                _state.update { it.copy(resetLoading = false, resetMessage = "Пароль изменён", resetCodeSent = false) }
            } catch (e: Exception) {
                _state.update { it.copy(resetLoading = false, resetError = "Неверный или просроченный код") }
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        if (newPassword.length < 8) {
            _state.update { it.copy(passwordError = "Новый пароль должен быть не менее 8 символов", passwordMessage = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(passwordLoading = true, passwordError = null, passwordMessage = null) }
            try {
                api.changePassword(mapOf("oldPassword" to oldPassword, "newPassword" to newPassword))
                _state.update { it.copy(passwordLoading = false, passwordMessage = "Пароль изменён") }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("401") == true) "Неверный текущий пароль"
                          else "Не удалось сменить пароль"
                _state.update { it.copy(passwordLoading = false, passwordError = msg) }
            }
        }
    }
}
