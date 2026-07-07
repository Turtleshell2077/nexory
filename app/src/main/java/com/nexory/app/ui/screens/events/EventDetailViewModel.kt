package com.nexory.app.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.EventDto
import com.nexory.app.data.network.ParticipantDto
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val event:        EventDto?            = null,
    val participants: List<ParticipantDto> = emptyList(),
    val isJoined:     Boolean         = false,
    val isMyEvent:    Boolean         = false,
    val eventChatId:  String?         = null,
    val isLoading:    Boolean         = false,
    val actionLoading: Boolean        = false,   // запись/отписка/создание чата
    val error:        String?         = null,
    // Когда != null — экран должен перейти в этот direct-чат с организатором
    val openDirectChatId: String?     = null,
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val api:          NexoryApi,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailUiState())
    val uiState = _state.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getEvent(eventId)
                val userId   = tokenManager.getUserId()
                _state.update {
                    it.copy(
                        event        = response.event,
                        participants = response.participants,
                        isJoined     = response.isJoined,
                        isMyEvent    = response.event.creatorId == userId,
                        eventChatId  = response.chatId,
                        isLoading    = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить мероприятие") }
            }
        }
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionLoading = true, error = null) }
            try {
                api.joinEvent(eventId)
                // Перезагружаем, чтобы получить chatId (теперь пользователь в чате) и счётчики
                loadEvent(eventId)
            } catch (e: Exception) {
                val msg = if (e.message?.contains("409") == true) "Мест больше нет" else "Не удалось записаться"
                _state.update { it.copy(actionLoading = false, error = msg) }
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionLoading = true, error = null) }
            try {
                api.leaveEvent(eventId)
                loadEvent(eventId)
            } catch (e: Exception) {
                _state.update { it.copy(actionLoading = false, error = "Не удалось отписаться") }
            }
        }
    }

    // Исключить участника (только создатель)
    fun kickParticipant(eventId: String, userId: String) {
        viewModelScope.launch {
            try { api.kickParticipant(eventId, userId); loadEvent(eventId) } catch (_: Exception) {}
        }
    }

    // Назначить/снять модератора
    fun setParticipantRole(eventId: String, userId: String, role: String) {
        viewModelScope.launch {
            try { api.setParticipantRole(eventId, userId, mapOf("role" to role)); loadEvent(eventId) } catch (_: Exception) {}
        }
    }

    // Удалить мероприятие (только создатель)
    fun deleteEvent(eventId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(actionLoading = true, error = null) }
            try {
                api.deleteEvent(eventId)
                onDeleted()
            } catch (e: Exception) {
                _state.update { it.copy(actionLoading = false, error = "Не удалось удалить") }
            }
        }
    }

    // Открыть личный чат с организатором
    fun messageCreator() {
        val creatorId = _state.value.event?.creatorId ?: return
        viewModelScope.launch {
            _state.update { it.copy(actionLoading = true, error = null) }
            try {
                val response = api.getOrCreateDirectChat(mapOf("peerId" to creatorId))
                _state.update { it.copy(actionLoading = false, openDirectChatId = response.chatId) }
            } catch (e: Exception) {
                _state.update { it.copy(actionLoading = false, error = "Не удалось открыть чат") }
            }
        }
    }

    fun consumeOpenChat() {
        _state.update { it.copy(openDirectChatId = null) }
    }
}
