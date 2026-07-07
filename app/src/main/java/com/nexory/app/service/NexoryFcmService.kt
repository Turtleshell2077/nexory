package com.nexory.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexory.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

@AndroidEntryPoint
class NexoryFcmService : FirebaseMessagingService() {

    // Retrofit напрямую через @Inject — без NexoryApi
    // чтобы не тянуть лишние зависимости
    @Inject lateinit var retrofit: Retrofit

    companion object {
        const val CHANNEL_ID = "nexory_notifications"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body  = message.notification?.body  ?: return
        showNotification(title, body, message.data["chatId"])
    }

    override fun onNewToken(token: String) {
        // Сохраняем новый FCM токен на нашем сервере
        CoroutineScope(Dispatchers.IO).launch {
            try {
                retrofit.create(com.nexory.app.data.network.NexoryApi::class.java)
                    .updateFcmToken(mapOf("fcmToken" to token))
            } catch (_: Exception) { }
        }
    }

    private fun showNotification(title: String, body: String, chatId: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID, "Nexory", NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("chatId", chatId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}