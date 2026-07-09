package com.nexory.app.data.network

import okhttp3.MultipartBody
import retrofit2.http.*

interface NexoryApi {

    // ---- Загрузка изображений ----
    @Multipart
    @POST("upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Map<String, String>


    // ---- Auth ----

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    // Обновить пару токенов — вызывается из AuthInterceptor при 401
    @POST("auth/refresh")
    suspend fun refreshTokens(@Body body: Map<String, String>): TokenResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, String>

    // Сброс пароля по коду из письма
    @POST("auth/request-password-reset")
    suspend fun requestPasswordReset(@Body body: Map<String, String>): Map<String, String>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Map<String, String>

    // Подтверждение почты
    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: Map<String, String>): Map<String, String>

    @POST("auth/resend-verification")
    suspend fun resendVerification(): Map<String, String>

    // ---- Events ----

    @GET("events")
    suspend fun getEvents(
        @Query("category") category: String? = null,
        @Query("search")   search:   String? = null,
        @Query("location") location: String? = null,
        @Query("sort")     sort:     String? = null,   // "soon" | "new"
        @Query("freeOnly") freeOnly: String? = null,   // "true"
        @Query("maxPrice") maxPrice: String? = null,
    ): FeedResponse

    // Мероприятия, на которые записан пользователь
    @GET("events/my")
    suspend fun getMyEvents(): Map<String, List<EventDto>>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: String): EventDetailResponse

    @POST("events")
    suspend fun createEvent(@Body body: Map<String, String?>): Map<String, EventDto>

    @PUT("events/{id}")
    suspend fun updateEvent(@Path("id") id: String, @Body body: Map<String, String?>): Map<String, EventDto>

    @POST("events/{id}/join")
    suspend fun joinEvent(@Path("id") id: String): Map<String, String>

    @DELETE("events/{id}/leave")
    suspend fun leaveEvent(@Path("id") id: String): Map<String, String>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): Map<String, String>

    // Управление участниками (только создатель)
    @DELETE("events/{id}/participants/{userId}")
    suspend fun kickParticipant(@Path("id") id: String, @Path("userId") userId: String): Map<String, String>

    @PUT("events/{id}/participants/{userId}")
    suspend fun setParticipantRole(@Path("id") id: String, @Path("userId") userId: String, @Body body: Map<String, String>): Map<String, String>

    // ---- Chats ----

    // Все чаты пользователя (список с last_message и unread_count)
    @GET("chats")
    suspend fun getChats(@Query("archived") archived: String? = null): Map<String, List<ChatDto>>

    // История сообщений — курсорная пагинация по UUID сообщения
    @GET("chats/{id}/messages")
    suspend fun getMessages(
        @Path("id")    chatId: String,
        @Query("before") before: String? = null,
        @Query("limit")  limit:  Int     = 30,
    ): Map<String, List<MessageDto>>

    // Создать или получить уже существующий direct-чат
    @POST("chats/direct")
    suspend fun getOrCreateDirectChat(@Body body: Map<String, String>): CreateDirectChatResponse

    // Информация о чате (заголовок, аватар)
    @GET("chats/{id}")
    suspend fun getChatInfo(@Path("id") chatId: String): ChatInfoResponse

    // Надёжная отправка сообщения через REST (гарантирует сохранение в БД)
    @POST("chats/{id}/messages")
    suspend fun sendMessageRest(
        @Path("id") chatId: String,
        @Body body: Map<String, String>,
    ): Map<String, MessageDto>

    // Сменить аватар чата
    @PUT("chats/{id}/avatar")
    suspend fun updateChatAvatar(
        @Path("id") chatId: String,
        @Body body: Map<String, String>,
    ): Map<String, String>

    // Заглушить / архивировать чат
    @PATCH("chats/{id}/flags")
    suspend fun updateChatFlags(
        @Path("id") chatId: String,
        @Body body: Map<String, String>,
    ): Map<String, String>

    // Удалить чат у себя (выйти)
    @DELETE("chats/{id}")
    suspend fun deleteChat(@Path("id") chatId: String): Map<String, String>

    // ---- Friends ----

    @GET("friends")
    suspend fun getFriends(): Map<String, List<FriendDto>>

    @GET("friends/requests")
    suspend fun getFriendRequests(): Map<String, List<FriendDto>>

    @POST("friends/request")
    suspend fun sendFriendRequest(@Body body: Map<String, String>): Map<String, String>

    @POST("friends/accept")
    suspend fun acceptFriendRequest(@Body body: Map<String, String>): Map<String, String>

    @DELETE("friends/{id}")
    suspend fun removeFriend(@Path("id") friendId: String): Map<String, String>

    // Отменить исходящую заявку в друзья
    @DELETE("friends/request/{id}")
    suspend fun cancelFriendRequest(@Path("id") userId: String): Map<String, String>

    // ---- Users ----

    @GET("users/me")
    suspend fun getMyProfile(): ProfileResponse

    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Map<String, Any>

    @PUT("users/me")
    suspend fun updateProfile(@Body body: Map<String, String?>): ProfileResponse

    @PUT("users/me/password")
    suspend fun changePassword(@Body body: Map<String, String>): Map<String, String>

    // Обновить FCM-токен — вызывается при получении нового токена из Firebase
    @PUT("users/me/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): Map<String, String>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Map<String, List<FriendDto>>

    // Выбранные друзья, кому виден профиль
    @GET("users/me/allowed")
    suspend fun getAllowedFriends(): Map<String, List<String>>

    @PUT("users/me/allowed")
    suspend fun setAllowedFriends(@Body body: Map<String, List<String>>): Map<String, Any>

    // ---- Support ----

    @POST("support")
    suspend fun createSupportTicket(@Body body: Map<String, String>): Map<String, String>
}