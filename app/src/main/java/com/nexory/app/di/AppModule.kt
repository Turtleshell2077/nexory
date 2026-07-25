package com.nexory.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nexory.app.data.network.AuthInterceptor
import com.nexory.app.data.network.NexoryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Единый источник адреса сервера — NexoryConfig (см. комментарий про HTTPS перед релизом)
    private val BASE_URL = com.nexory.app.NexoryConfig.API_BASE_URL

    // Gson с настройками:
    // lenient — не падает на некоторые нестандартные JSON
    @Provides @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    // OkHttpClient БЕЗ авторизации — только для /auth/refresh
    // Если добавить AuthInterceptor сюда, получим рекурсию при 401
    @Provides @Singleton @Named("authClient")
    fun provideAuthOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit БЕЗ авторизации — инжектируется в TokenManager
    @Provides @Singleton @Named("authRetrofit")
    fun provideAuthRetrofit(
        gson: Gson,
        @Named("authClient") client: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // Основной OkHttpClient С авторизацией и логированием
    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // В release полностью выключаем: уровень BODY пишет в logcat токены
            // авторизации и содержимое личных сообщений — это утечка и повод
            // для замечаний при проверке безопасности.
            level = if (com.nexory.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // добавляет Authorization header
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Основной Retrofit — используется везде кроме refresh токена
    @Provides @Singleton
    fun provideRetrofit(gson: Gson, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // API-интерфейс — Retrofit генерирует реализацию
    @Provides @Singleton
    fun provideNexoryApi(retrofit: Retrofit): NexoryApi =
        retrofit.create(NexoryApi::class.java)
}