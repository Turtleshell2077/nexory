package com.nexory.app.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiState(
    val isLoading: Boolean = false,
    val isSent:    Boolean = false,
    val error:     String? = null,
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {

    private val _state = MutableStateFlow(SupportUiState())
    val state = _state.asStateFlow()

    fun send(subject: String, body: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                api.createSupportTicket(mapOf("subject" to subject, "body" to body))
                _state.update { it.copy(isLoading = false, isSent = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Ошибка отправки: ${e.message}") }
            }
        }
    }
}