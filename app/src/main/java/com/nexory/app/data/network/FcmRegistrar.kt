package com.nexory.app.data.network

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Отправляет текущий FCM-токен устройства на сервер. Раньше токен уходил только
// в onNewToken (при первой установке), из-за чего у уже залогиненных пользователей
// сервер не знал адрес устройства и push не приходил. Теперь вызываем при входе
// и старте приложения.
@Singleton
class FcmRegistrar @Inject constructor(
    private val api: NexoryApi,
) {
    fun register() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isNullOrBlank()) return@addOnSuccessListener
            CoroutineScope(Dispatchers.IO).launch {
                try { api.updateFcmToken(mapOf("fcmToken" to token)) } catch (_: Exception) {}
            }
        }
    }
}
