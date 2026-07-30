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
    val tab:           Int             = 0,  // 0=друзья, 1=запросы
    val isLoading:     Boolean         = false,
    val isSearching:   Boolean         = false, // идёт запрос поиска
    val searchOpen:    Boolean         = false, // раскрыта ли инлайн-строка поиска
    val sentRequests:  Set<String>     = emptySet(),  // кому уже отправили заявку
    val myUserId:      String?         = null,
) {
    /** Ник введён, но никого не нашли — показываем «не найдено». */
    val searchEmpty: Boolean
        get() = searchOpen && !isSearching && searchQuery.trim().length >= 2 && searchResults.isEmpty()
}

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
        // Ник вводят без «@», но если пользователь его напечатал — не мешаем
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        val trimmed = query.trim().removePrefix("@")
        if (trimmed.length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        // Дебаунс + отмена предыдущего запроса — устраняет гонки и мигание результатов
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            delay(300)
            try {
                val results = api.searchUsers(trimmed)["users"] ?: emptyList()
                _state.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    /** Раскрыть/свернуть инлайн-строку поиска на вкладке «Друзья». */
    fun toggleSearch() {
        _state.update {
            if (it.searchOpen) {
                // Закрываем — сбрасываем запрос и результаты
                searchJob?.cancel()
                it.copy(searchOpen = false, searchQuery = "", searchResults = emptyList(), isSearching = false)
            } else {
                it.copy(searchOpen = true)
            }
        }
    }

    fun sendRequest(userId: String) {
        // Оптимистично помечаем как отправленную — мгновенный отклик на нажатие.
        // Долговременное состояние приходит с сервера полем friend_status, поэтому
        // «Заявка отправлена» переживает перезапуск приложения.
        _state.update { it.copy(sentRequests = it.sentRequests + userId) }
        viewModelScope.launch {
            try {
                api.sendFriendRequest(mapOf("addresseeId" to userId))
                // Заявка могла оказаться встречной и сразу стать дружбой —
                // перечитываем, чтобы на экране было реальное положение дел
                load()
                search(_state.value.searchQuery)
            } catch (_: Exception) {
                _state.update { it.copy(sentRequests = it.sentRequests - userId) }
            }
        }
    }

    fun acceptRequest(requesterId: String) {
        viewModelScope.launch {
            try {
                api.acceptFriendRequest(mapOf("requesterId" to requesterId))
                load()
                search(_state.value.searchQuery)
            } catch (_: Exception) {}
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