package com.nexory.app.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.FriendDto
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val friends:       List<FriendDto> = emptyList(),
    val requests:      List<FriendDto> = emptyList(),
    val searchResults: List<FriendDto> = emptyList(),
    val searchQuery:   String          = "",
    val tab:           Int             = 0,  // 0=друзья, 1=запросы, 2=поиск
    val isLoading:     Boolean         = false,
    val sentRequests:  Set<String>     = emptySet(),  // кому уже отправили заявку
    val myUserId:      String?         = null,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val api: NexoryApi,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FriendsUiState())
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { _state.update { it.copy(myUserId = tokenManager.getUserId()) } }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val friends  = api.getFriends()["friends"]        ?: emptyList()
                val requests = api.getFriendRequests()["requests"] ?: emptyList()
                _state.update { it.copy(friends = friends, requests = requests, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private var searchJob: Job? = null

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }

        // Дебаунс + отмена предыдущего запроса — устраняет гонки и мигание результатов
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val results = api.searchUsers(trimmed)["users"] ?: emptyList()
                _state.update { it.copy(searchResults = results) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                _state.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    fun sendRequest(userId: String) {
        // Оптимистично помечаем как отправленную
        _state.update { it.copy(sentRequests = it.sentRequests + userId) }
        viewModelScope.launch {
            try {
                api.sendFriendRequest(mapOf("addresseeId" to userId))
            } catch (_: Exception) {
                _state.update { it.copy(sentRequests = it.sentRequests - userId) }
            }
        }
    }

    fun acceptRequest(requesterId: String) {
        viewModelScope.launch {
            try { api.acceptFriendRequest(mapOf("requesterId" to requesterId)); load() } catch (_: Exception) {}
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            try { api.removeFriend(friendId); load() } catch (_: Exception) {}
        }
    }

    fun setTab(tab: Int) = _state.update { it.copy(tab = tab) }

    // Открыть/создать личный чат с пользователем и вернуть его id
    fun openDirectChat(userId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.getOrCreateDirectChat(mapOf("peerId" to userId))
                onReady(response.chatId)
            } catch (_: Exception) {}
        }
    }
}