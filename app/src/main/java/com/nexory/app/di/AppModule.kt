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

    // Базовый URL — в реальном проекте берём из BuildConfig
    // BuildConfig.API_BASE_URL задаётся в build.gradle:
    //   buildConfigField "String", "API_BASE_URL", '"https://api.nexory.app/api/v1/"'
    private const val BASE_URL = "http://186.246.12.170:3000/api/v1/"

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
            // В релизе отключить! Логи раскрывают токены
            level = HttpLoggingInterceptor.Level.BODY
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