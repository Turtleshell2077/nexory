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

        // Если сервер ответил 401 — токен истёк, пробуем обновить
        if (response.code == 401) {
            response.close()

            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
                ?: return response // Refresh тоже нет — пользователь не залогинен

            // Запрашиваем новые токены. runBlocking здесь допустим,
            // т.к. мы уже на IO-потоке OkHttp.
            val newTokens = runBlocking {
                try {
                    // Прямой HTTP запрос без интерсептора (authRetrofit)
                    tokenManager.refreshAndSave(refreshToken)
                } catch (e: Exception) {
                    null // Refresh тоже не удался → разлогиниваем
                }
            }

            return if (newTokens != null) {
                // Повторяем оригинальный запрос с новым токеном
                val retryRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newTokens")
                    .build()
                chain.proceed(retryRequest)
            } else {
                // Оба токена недействительны — чистим данные
                runBlocking { tokenManager.clear() }
                response
            }
        }

        return response
    }
}