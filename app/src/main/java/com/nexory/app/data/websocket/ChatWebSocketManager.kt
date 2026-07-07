package com.nexory.app.data.websocket

import com.google.gson.Gson
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.WsNewMessagePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

// Запечатанный класс для типобезопасных событий WebSocket.
// Когда добавим новый тип события — компилятор потребует обработать его.
sealed class WsEvent {
    data class NewMessage(val message: WsNewMessagePayload) : WsEvent()
    object Connected   : WsEvent()
    object Disconnected: WsEvent()
}

@Singleton
class ChatWebSocketManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {
    companion object {
        // WebSocket-адрес должен указывать на тот же backend, что и REST (AppModule.BASE_URL).
        // Для локального dev-сервера это ws:// на тот же хост и порт.
        private const val WS_BASE_URL = "ws://192.168.1.104:3000/ws"
        private const val MAX_RETRIES = 5
    }

    private var webSocket: WebSocket? = null
    private var retryCount = 0

    // SharedFlow — шина событий. Все подписчики получают каждое событие.
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    // Подключение к WebSocket с текущим access token
    fun connect() {
        scope.launch {
            val token = tokenManager.getAccessToken() ?: return@launch

            val request = Request.Builder()
                .url("$WS_BASE_URL?token=$token")
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    retryCount = 0
                    _events.tryEmit(WsEvent.Connected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        // Парсим тип события из JSON
                        val raw = gson.fromJson(text, Map::class.java)
                        when (raw["type"] as? String) {
                            "new_message" -> {
                                val payload = gson.fromJson(text, WsNewMessagePayload::class.java)
                                _events.tryEmit(WsEvent.NewMessage(payload))
                            }
                            // Другие типы событий добавляем здесь
                        }
                    } catch (_: Exception) { /* игнорируем невалидный JSON */ }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _events.tryEmit(WsEvent.Disconnected)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _events.tryEmit(WsEvent.Disconnected)
                    // Exponential backoff реконнект: 1с, 2с, 4с, 8с, 16с
                    if (retryCount < MAX_RETRIES) {
                        scope.launch {
                            delay((1000L * (1 shl retryCount)).coerceAtMost(30_000L))
                            retryCount++
                            connect()
                        }
                    }
                }
            })
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Logout")
        webSocket = null
        retryCount = 0
    }

    // Отправить текстовое сообщение в чат
    fun sendMessage(chatId: String, content: String) {
        val payload = gson.toJson(mapOf(
            "type"    to "send_message",
            "chatId"  to chatId,
            "content" to content,
            "msgType" to "text",
        ))
        webSocket?.send(payload)
    }

    // Уведомить сервер, что пользователь открыл чат и прочитал сообщения
    fun markRead(chatId: String) {
        val payload = gson.toJson(mapOf(
            "type"   to "mark_read",
            "chatId" to chatId,
        ))
        webSocket?.send(payload)
    }
}