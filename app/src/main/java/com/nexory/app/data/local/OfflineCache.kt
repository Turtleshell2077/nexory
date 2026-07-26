package com.nexory.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexory.app.data.local.db.CacheDao
import com.nexory.app.data.local.db.CachedJsonEntity
import com.nexory.app.data.network.ChatDto
import com.nexory.app.data.network.EventDto
import com.nexory.app.data.network.MessageDto
import com.nexory.app.data.network.UserDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Типизированный фасад над [CacheDao].
 *
 * Экраны и ViewModel работают с DTO и ничего не знают ни про Room, ни про JSON.
 * Все операции «мягкие»: любая ошибка чтения/записи кэша не должна ломать основной
 * поток — кэш это удобство, а не источник истины.
 */
@Singleton
class OfflineCache @Inject constructor(
    private val dao: CacheDao,
    private val gson: Gson,
) {
    private companion object {
        const val KEY_FEED_UPCOMING = "feed:upcoming"
        const val KEY_FEED_PAST     = "feed:past"
        const val KEY_MY_EVENTS     = "feed:my"
        const val KEY_PROFILE_ME    = "profile:me"
        const val KEY_CHATS         = "chats:main"
        fun messagesKey(chatId: String) = "messages:$chatId"
    }

    private val eventListType   = object : TypeToken<List<EventDto>>() {}.type
    private val messageListType = object : TypeToken<List<MessageDto>>() {}.type
    private val chatListType    = object : TypeToken<List<ChatDto>>() {}.type

    // ---- Лента мероприятий ----

    suspend fun saveFeed(upcoming: List<EventDto>, past: List<EventDto>) {
        put(KEY_FEED_UPCOMING, upcoming)
        put(KEY_FEED_PAST, past)
    }

    suspend fun loadFeedUpcoming(): List<EventDto> = getList(KEY_FEED_UPCOMING, eventListType)
    suspend fun loadFeedPast(): List<EventDto> = getList(KEY_FEED_PAST, eventListType)

    suspend fun saveMyEvents(events: List<EventDto>) = put(KEY_MY_EVENTS, events)
    suspend fun loadMyEvents(): List<EventDto> = getList(KEY_MY_EVENTS, eventListType)

    /** Когда лента была закэширована (мс) или null, если кэша нет. */
    suspend fun feedCachedAt(): Long? = runCatching { dao.get(KEY_FEED_UPCOMING)?.updatedAt }.getOrNull()

    // ---- Профиль ----

    suspend fun saveMyProfile(user: UserDto) = put(KEY_PROFILE_ME, user)

    suspend fun loadMyProfile(): UserDto? = runCatching {
        dao.get(KEY_PROFILE_ME)?.json?.let { gson.fromJson(it, UserDto::class.java) }
    }.getOrNull()

    // ---- Список чатов ----

    suspend fun saveChats(chats: List<ChatDto>) = put(KEY_CHATS, chats)
    suspend fun loadChats(): List<ChatDto> = getList(KEY_CHATS, chatListType)

    // ---- Сообщения конкретного чата ----

    suspend fun saveMessages(chatId: String, messages: List<MessageDto>) {
        // Ограничиваем размер кэша: последние 100 сообщений на чат достаточно
        // для просмотра истории без сети и не раздувает базу.
        put(messagesKey(chatId), messages.takeLast(100))
    }

    suspend fun loadMessages(chatId: String): List<MessageDto> =
        getList(messagesKey(chatId), messageListType)

    // ---- Очистка ----

    /** Вызывается при выходе из аккаунта: чужие данные не должны остаться на устройстве. */
    suspend fun clearAll() { runCatching { dao.clearAll() } }

    // ---- Внутреннее ----

    private suspend fun put(key: String, value: Any) {
        runCatching {
            dao.put(CachedJsonEntity(key = key, json = gson.toJson(value), updatedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun <T> getList(key: String, type: java.lang.reflect.Type): List<T> =
        runCatching {
            val json = dao.get(key)?.json ?: return emptyList()
            gson.fromJson<List<T>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
}
