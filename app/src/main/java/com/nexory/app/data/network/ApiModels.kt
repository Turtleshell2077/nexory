package com.nexory.app.data.network

import com.google.gson.annotations.SerializedName

// ---- Авторизация ----

// Ответ на /auth/login и /auth/register
data class AuthResponse(
    val user:         UserDto,
    @SerializedName("accessToken")  val accessToken:  String,
    @SerializedName("refreshToken") val refreshToken: String,
)

// Ответ на /auth/refresh
data class TokenResponse(
    @SerializedName("accessToken")  val accessToken:  String,
    @SerializedName("refreshToken") val refreshToken: String,
)

// ---- Пользователь ----

data class UserDto(
    val id:         String,
    val username:   String,
    val email:      String?     = null,
    val phone:      String?     = null,
    @SerializedName("avatar_url")   val avatarUrl:   String? = null,
    val bio:        String?     = null,
    val role:       String      = "user",
    @SerializedName("created_at")   val createdAt:   String? = null,
    // Расширенные поля профиля
    val age:        Int?        = null,
    val country:    String?     = null,
    val city:       String?     = null,
    @SerializedName("display_name") val displayName: String? = null,
    val sports:     String?     = null,   // через запятую: "Футбол, Бег"
    @SerializedName("looking_for")  val lookingFor:  String? = null,
    @SerializedName("activity")     val activity:    String? = null,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerializedName("notify_messages")       val notifyMessages:       Boolean = true,
    @SerializedName("notify_friend_events")  val notifyFriendEvents:   Boolean = true,
    @SerializedName("notify_interest_events") val notifyInterestEvents: Boolean = true,
    @SerializedName("contacts_public")       val contactsPublic:       Boolean = false,
    @SerializedName("profile_visibility")    val profileVisibility:    String  = "friends", // all|friends|selected
    @SerializedName("friend_status")         val friendStatus:         String? = null, // self|friends|pending_out|pending_in|none
)

// Используется в списке друзей — сервер возвращает чуть меньше полей
typealias FriendDto = UserDto

// Ответ на GET /users/me и PUT /users/me — сервер возвращает { user, events }.
// Объявляем только нужное поле user; events Gson игнорирует.
data class ProfileResponse(
    val user: UserDto? = null,
)

// ---- Мероприятия ----

data class EventDto(
    val id:          String,
    val title:       String,
    val description: String?  = null,
    val address:     String,
    @SerializedName("cover_url")         val coverUrl:        String? = null,
    val category:    String?  = null,
    @SerializedName("starts_at")         val startsAt:        String,
    @SerializedName("ends_at")           val endsAt:          String? = null,
    @SerializedName("max_participants")  val maxParticipants: Int?    = null,
    @SerializedName("participant_count") val participantCount: Int    = 0,
    @SerializedName("is_private")        val isPrivate:       Boolean = false,
    val price:       Double?  = null,
    @SerializedName("skill_level")       val skillLevel:      String? = null,
    @SerializedName("event_type")        val eventType:       String? = null,
    @SerializedName("price_description") val priceDescription: String? = null,
    val status:      String   = "active",
    // Создатель — denormalized для удобства UI
    @SerializedName("creator_id")       val creatorId:       String? = null,
    @SerializedName("creator_username") val creatorUsername:  String  = "",
    @SerializedName("creator_avatar")   val creatorAvatar:   String? = null,
)

// Ответ на GET /events — лента разбита на предстоящие и прошедшие
data class FeedResponse(
    val upcoming: List<EventDto> = emptyList(),
    val past:     List<EventDto> = emptyList(),
)

// Участник мероприятия (с ролью)
data class ParticipantDto(
    val id:       String,
    val username: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val role:     String = "participant",   // participant | moderator
)

// Ответ на GET /events/{id} — детальная страница
data class EventDetailResponse(
    val event:        EventDto,
    val participants: List<ParticipantDto> = emptyList(),
    val isJoined:     Boolean       = false,
    val chatId:       String?       = null,
)

// ---- Чаты ----

data class ChatDto(
    val id:       String,
    val type:     String,         // "direct" | "event" | "support"
    @SerializedName("event_id")    val eventId:    String?       = null,
    @SerializedName("avatar_url")  val avatarUrl:  String?       = null,
    val peer:                      PeerDto?        = null,   // для direct-чата
    @SerializedName("event_info")  val eventInfo:  EventInfoDto? = null, // для event-чата
    @SerializedName("last_message") val lastMessage: LastMessageDto? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("last_read_at") val lastReadAt:  String? = null,
)

// Информация о чате для шапки экрана (GET /chats/{id})
data class ChatInfo(
    val id:            String,
    val type:          String,
    val title:         String  = "",
    @SerializedName("avatar_url")      val avatarUrl:     String? = null,
    @SerializedName("can_edit_avatar") val canEditAvatar: Boolean = false,
)

data class ChatInfoResponse(
    val chat:    ChatInfo,
    val members: List<UserDto> = emptyList(),
    val event:   EventDto?     = null,
)

// Собеседник в direct-чате
data class PeerDto(
    val id:         String,
    val username:   String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
)

// Краткая инфо о мероприятии для чата
data class EventInfoDto(
    val id:    String,
    val title: String,
)

// Последнее сообщение в чате (для списка чатов)
data class LastMessageDto(
    val id:       String,
    val content:  String,
    @SerializedName("created_at")       val createdAt:       String,
    @SerializedName("sender_username")  val senderUsername:  String,
)

// ---- Сообщения ----

data class MessageDto(
    val id:       String,
    @SerializedName("chat_id")    val chatId:    String  = "",
    val content:  String,
    val type:     String  = "text",
    @SerializedName("is_deleted") val isDeleted: Boolean = false,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("sender_id")        val senderId:       String  = "",
    @SerializedName("sender_username")  val senderUsername:  String  = "",
    @SerializedName("sender_avatar")    val senderAvatar:    String? = null,
)

// Обёртка WebSocket-события "new_message"
data class WsNewMessagePayload(
    val message: MessageDto? = null,
)

// ---- Создание чата ----

data class CreateDirectChatResponse(
    @SerializedName("chatId") val chatId: String,
    @SerializedName("isNew")  val isNew:  Boolean,
)