package com.nexory.app.data.local.db

import android.content.Context
import androidx.room.*

/**
 * Локальный кэш для оффлайн-режима.
 *
 * Решение по схеме: данные хранятся как JSON-строки, а не как «широкие» таблицы с
 * колонкой на каждое поле DTO. Причины:
 *  - кэшу не нужны запросы по полям: нам требуется только «отдай последнюю ленту»
 *    и «отдай сообщения чата X» — это выборка по первичному ключу;
 *  - при добавлении поля в DTO (а мы их добавляли много: metro, created_at и т.д.)
 *    схема БД не меняется и не нужна миграция Room;
 *  - меньше кода маппинга — меньше мест, где кэш может разойтись с сервером.
 *
 * Кэш неавторитетен: он всегда перезаписывается свежими данными при успешном
 * запросе и используется только когда сети нет.
 */

@Entity(tableName = "cached_json")
data class CachedJsonEntity(
    /** Составной ключ вида "feed:upcoming", "profile:me", "messages:<chatId>". */
    @PrimaryKey val key: String,
    /** Сериализованный JSON (объект или массив). */
    val json: String,
    /** Когда закэшировано — показываем пользователю актуальность данных. */
    val updatedAt: Long,
)

@Dao
interface CacheDao {

    @Query("SELECT * FROM cached_json WHERE key = :key LIMIT 1")
    suspend fun get(key: String): CachedJsonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CachedJsonEntity)

    @Query("DELETE FROM cached_json WHERE key = :key")
    suspend fun delete(key: String)

    /** Полная очистка — вызывается при выходе из аккаунта и удалении аккаунта. */
    @Query("DELETE FROM cached_json")
    suspend fun clearAll()

    /** Удалить кэш сообщений всех чатов (например, при смене пользователя). */
    @Query("DELETE FROM cached_json WHERE key LIKE 'messages:%'")
    suspend fun clearMessages()
}

@Database(entities = [CachedJsonEntity::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao

    companion object {
        const val NAME = "nexory_cache.db"
    }
}
