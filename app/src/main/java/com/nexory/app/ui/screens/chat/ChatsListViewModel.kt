package com.nexory.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.network.ChatDto
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatsListUiState(
    val chats:         List<ChatDto> = emptyList(),
    val isLoading:     Boolean       = false,
    val selectionMode: Boolean       = false,
    val selected:      Set<String>   = emptySet(),
)

@HiltViewModel
class ChatsListViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatsListUiState())
    val state = _state.asStateFlow()

    private var archived = false

    init { load() }

    fun load(archived: Boolean = this.archived) {
        this.archived = archived
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val chats = api.getChats(if (archived) "true" else null)["chats"] ?: emptyList()
                _state.update { it.copy(chats = chats, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ---- Режим выбора ----
    fun enterSelection(firstId: String? = null) {
        _state.update { it.copy(selectionMode = true, selected = firstId?.let { id -> setOf(id) } ?: emptySet()) }
    }
    fun exitSelection() {
        _state.update { it.copy(selectionMode = false, selected = emptySet()) }
    }
    fun toggleSelect(id: String) {
        _state.update {
            val s = if (id in it.selected) it.selected - id else it.selected + id
            it.copy(selected = s)
        }
    }

    fun archiveSelected() {
        val ids = _state.value.selected
        viewModelScope.launch {
            ids.forEach { id -> try { api.updateChatFlags(id, mapOf("archived" to "true")) } catch (_: Exception) {} }
            exitSelection(); load()
        }
    }
    fun deleteSelected() {
        val ids = _state.value.selected
        viewModelScope.launch {
            ids.forEach { id -> try { api.deleteChat(id) } catch (_: Exception) {} }
            exitSelection(); load()
        }
    }

    // ---- Одиночные действия (long-press меню) ----
    fun setMuted(chatId: String, muted: Boolean) {
        viewModelScope.launch {
            try { api.updateChatFlags(chatId, mapOf("muted" to muted.toString())) } catch (_: Exception) {}
            load()
        }
    }
    fun archive(chatId: String) {
        viewModelScope.launch {
            try { api.updateChatFlags(chatId, mapOf("archived" to "true")) } catch (_: Exception) {}
            load()
        }
    }
    fun unarchive(chatId: String) {
        viewModelScope.launch {
            try { api.updateChatFlags(chatId, mapOf("archived" to "false")) } catch (_: Exception) {}
            load()
        }
    }
    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            try { api.deleteChat(chatId) } catch (_: Exception) {}
            load()
        }
    }
}
