package com.nexory.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexory.app.data.network.TokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// Расширение для создания DataStore — одно хранилище на приложение
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "nexory_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    // Отдельный Retrofit без AuthInterceptor — для запроса /auth/refresh.
    // Если использовать основной, попадём в рекурсивный цикл 401.
    @Named("authRetrofit") private val authRetrofit: Retrofit,
) {
    companion object {
        private val KEY_ACCESS_TOKEN  = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID       = stringPreferencesKey("user_id")
    }

    // Flow — реактивный поток. MainActivity подписывается на него,
    // чтобы автоматически перенаправлять на Login при разлогине.
    fun isLoggedIn(): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_ACCESS_TOKEN] != null
        }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[KEY_ACCESS_TOKEN]

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[KEY_REFRESH_TOKEN]

    suspend fun getUserId(): String? =
        context.dataStore.data.first()[KEY_USER_ID]

    // Сохраняем все три значения атомарно в одной транзакции
    suspend fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID]       = userId
        }
    }

    // Вызывается из AuthInterceptor при 401.
    // Возвращает новый access token или null при ошибке.
    suspend fun refreshAndSave(refreshToken: String): String? {
        return try {
            // Прямой вызов auth API без интерсептора
            val authApi = authRetrofit.create(
                com.nexory.app.data.network.NexoryApi::class.java
            )
            val response: TokenResponse = authApi.refreshTokens(
                mapOf("refreshToken" to refreshToken)
            )
            // Сохраняем новые токены — userId не меняется
            val currentUserId = getUserId() ?: ""
            saveTokens(response.accessToken, response.refreshToken, currentUserId)
            response.accessToken
        } catch (e: Exception) {
            null
        }
    }

    // Полная очистка при logout или невалидном refresh token
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}