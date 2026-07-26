package com.nexory.app.di

import android.content.Context
import androidx.room.Room
import com.nexory.app.data.local.db.CacheDao
import com.nexory.app.data.local.db.CacheDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Room-база локального кэша для оффлайн-режима. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCacheDatabase(@ApplicationContext context: Context): CacheDatabase =
        Room.databaseBuilder(context, CacheDatabase::class.java, CacheDatabase.NAME)
            // Кэш неавторитетен: при изменении схемы проще выбросить его целиком,
            // чем писать миграции. Данные всё равно перезагрузятся с сервера.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCacheDao(db: CacheDatabase): CacheDao = db.cacheDao()
}
