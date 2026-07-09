package com.nexory.app

import android.app.Application
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.FcmRegistrar
import com.nexory.app.data.websocket.ChatWebSocketManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NexoryApp : Application() {

    // Hilt инжектирует зависимости прямо в Application
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var wsManager:    ChatWebSocketManager
    @Inject lateinit var fcmRegistrar: FcmRegistrar

    override fun onCreate() {
        super.onCreate()

        // Если пользователь уже залогинен (токен есть в DataStore) —
        // подключаем WebSocket и обновляем FCM-токен на сервере
        CoroutineScope(Dispatchers.IO).launch {
            val token = tokenManager.getAccessToken()
            if (token != null) {
                wsManager.connect()
                fcmRegistrar.register()
            }
        }
    }
}
