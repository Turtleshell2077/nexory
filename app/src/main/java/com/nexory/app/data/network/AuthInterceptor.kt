package com.nexory.app.data.network

import com.nexory.app.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenManager.getAccessToken() }

        // Строим запрос с токеном
        val request = chain.request().newBuilder().apply {
            if (accessToken != null) {
                header("Authorization", "Bearer $accessToken")
            }
        }.build()

        val response = chain.proceed(request)

        // Если сервер ответил 401 — токен истёк, пробуем обновить.
        // refreshIfNeeded сериализует параллельные обновления через мьютекс
        // и переиспользует уже обновлённый токен, если другой запрос успел раньше.
        if (response.code == 401) {
            response.close()

            val newAccessToken = runBlocking {
                try {
                    tokenManager.refreshIfNeeded(accessToken)
                } catch (e: Exception) {
                    null
                }
            }

            return if (newAccessToken != null) {
                // Повторяем оригинальный запрос с актуальным токеном
                val retryRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                chain.proceed(retryRequest)
            } else {
                // Сессия действительно мертва — чистим данные (уводит на экран входа)
                runBlocking { tokenManager.clear() }
                response
            }
        }

        return response
    }
}