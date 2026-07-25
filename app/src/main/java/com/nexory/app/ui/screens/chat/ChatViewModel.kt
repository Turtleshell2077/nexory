package com.nexory.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.MessageDto
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.data.websocket.ChatWebSocketManager
import com.nexory.app.data.websocket.WsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages:        List<MessageDto> = emptyList(),
    val chatTitle:       String           = "",
    val chatAvatarUrl:   String?          = null,
    val canEditAvatar:   Boolean          = false,
    val currentUserId:   String           = "",
    val isLoading:       Boolean          = false,
    val oldestMessageId: String?          = null,
    val hasMore:         Boolean          = true,
    /** Сообщения показаны из локального кэша — отправка недоступна до появления сети. */
    val isFromCache:     Boolean          = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api:          NexoryApi,
    private val wsManager:    ChatWebSocketManager,
    private val tokenManager: TokenManager,
    private val cache:        com.nexory.app.data.local.OfflineCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var currentChatId: String = ""
    private var wsCollectStarted = false

    fun loadChat(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: ""
            _uiState.update { it.copy(currentUserId = userId, isLoading = true) }

            // Информация о чате (заголовок + аватар)
            try {
                val info = api.getChatInfo(chatId).chat
                _uiState.update {
                    it.copy(
                        chatTitle     = info.title,
                        chatAvatarUrl = info.avatarUrl,
                        canEditAvatar = info.canEditAvatar,
                    )
                }
            } catch (_: Exception) { /* заголовок не критичен */ }

            try {
                val messages = api.getMessages(chatId)["messages"] ?: emptyList()
                cache.saveMessages(chatId, messages)
                _uiState.update {
                    it.copy(
                        messages        = messages,
                        isLoading       = false,
                        oldestMessageId = messages.firstOrNull()?.id,
                        hasMore         = messages.size >= 30,
                        isFromCache     = false,
                    )
                }
                wsManager.markRead(chatId)
            } catch (e: Exception) {
                // Оффлайн: показываем последние сохранённые сообщения только для чтения.
                // Отправка при этом останется недоступной (см. isFromCache в UI).
                val cached = cache.loadMessages(chatId)
                _uiState.update {
                    it.copy(
                        messages    = cached,
                        isLoading   = false,
                        hasMore     = false,       // подгружать историю без сети нельзя
                        isFromCache = cached.isNotEmpty(),
                    )
                }
            }
        }

        // Подписка на входящие сообщения (один раз на ViewModel)
        if (!wsCollectStarted) {
            wsCollectStarted = true
            viewModelScope.launch {
                wsManager.events.collect { event ->
                    if (event is WsEvent.NewMessage) {
                        val msg = event.message.message ?: return@collect
                        if (msg.chatId == currentChatId) {
                            _uiState.update { state ->
                                if (state.messages.any { it.id == msg.id }) state
                                else state.copy(messages = state.messages + msg)
                            }
                            wsManager.markRead(currentChatId)
                        }
                    }
                }
            }
        }
    }

    fun loadMoreMessages() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val older = api.getMessages(chatId = currentChatId, before = state.oldestMessageId)["messages"] ?: emptyList()
                _uiState.update {
                    it.copy(
                        messages        = older + it.messages,
                        isLoading       = false,
                        oldestMessageId = older.firstOrNull()?.id ?: it.oldestMessageId,
                        hasMore         = older.size >= 30,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        // Оптимистичный UI: показываем сообщение сразу с временным id
        val tempId = "temp_${System.currentTimeMillis()}"
        val tempMsg = MessageDto(
            id             = tempId,
            chatId         = chatId,
            content        = content,
            createdAt      = java.time.Instant.now().toString(),
            senderId       = _uiState.value.currentUserId,
            senderUsername = "Вы",
        )
        _uiState.update { it.copy(messages = it.messages + tempMsg) }

        // Надёжная отправка через REST — гарантирует сохранение в БД.
        viewModelScope.launch {
            try {
                val saved = api.sendMessageRest(chatId, mapOf("content" to content))["message"]
                _uiState.update { state ->
                    // Заменяем временное сообщение реальным (если оно ещё не пришло через WS)
                    val withoutTemp = state.messages.filterNot { it.id == tempId }
                    if (saved != null && withoutTemp.none { it.id == saved.id })
                        state.copy(messages = withoutTemp + saved)
                    else
                        state.copy(messages = withoutTemp)
                }
            } catch (e: Exception) {
                // Помечаем сбой отправки — удаляем оптимистичное сообщение
                _uiState.update { state ->
                    state.copy(messages = state.messages.filterNot { it.id == tempId })
                }
            }
        }
    }

    fun changeChatAvatar(avatarUrl: String) {
        viewModelScope.launch {
            try {
                api.updateChatAvatar(currentChatId, mapOf("avatar_url" to avatarUrl))
                _uiState.update { it.copy(chatAvatarUrl = avatarUrl) }
            } catch (_: Exception) {}
        }
    }
}
