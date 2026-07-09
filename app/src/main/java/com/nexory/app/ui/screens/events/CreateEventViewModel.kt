package com.nexory.app.ui.screens.events

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.network.EventDto
import com.nexory.app.data.network.MediaUploader
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateEventUiState(
    val isLoading: Boolean   = false,
    val isCreated: Boolean   = false,
    val error:     String?   = null,
    val loaded:    EventDto?  = null,   // для режима редактирования
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val api: NexoryApi,
    private val uploader: MediaUploader,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEventUiState())
    val uiState = _state.asStateFlow()

    suspend fun uploadImage(uri: Uri): String? = uploader.upload(uri)

    // Загрузить существующее мероприятие для редактирования
    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            try {
                val event = api.getEvent(eventId).event
                _state.update { it.copy(loaded = event) }
            } catch (_: Exception) {}
        }
    }

    // Создать или (если eventId != null) обновить мероприятие
    fun save(
        eventId: String?,
        title: String,
        description: String?,
        address: String,
        category: String?,
        startsAt: String,
        endsAt: String?,
        maxParticipants: Int?,
        isPrivate: Boolean,
        coverUrl: String?,
        price: Double?,
        skillLevel: String?,
        eventType: String?,
        priceDescription: String?,
        metro: String? = null,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val body = buildMap<String, String?> {
                    put("title",      title)
                    put("address",    address)
                    put("starts_at",  startsAt)
                    put("is_private", isPrivate.toString())
                    put("price",      (price ?: 0.0).toString())
                    if (!description.isNullOrBlank()) put("description",      description)
                    if (!category.isNullOrBlank())    put("category",         category)
                    if (!endsAt.isNullOrBlank())      put("ends_at",          endsAt)
                    if (maxParticipants != null)      put("max_participants", maxParticipants.toString())
                    if (!coverUrl.isNullOrBlank())    put("cover_url",        coverUrl)
                    if (!skillLevel.isNullOrBlank())  put("skill_level",      skillLevel)
                    if (!eventType.isNullOrBlank())   put("event_type",       eventType)
                    if (!priceDescription.isNullOrBlank()) put("price_description", priceDescription)
                    if (!metro.isNullOrBlank())       put("metro",            metro)
                }
                if (eventId == null) api.createEvent(body) else api.updateEvent(eventId, body)
                _state.update { it.copy(isLoading = false, isCreated = true) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                val msg = when {
                    e.message?.contains("400") == true -> "Проверь правильность данных"
                    e.message?.contains("401") == true -> "Нет прав. Попробуй заново войти"
                    e.message?.contains("403") == true -> "Можно редактировать только свои мероприятия"
                    else -> "Ошибка: ${e.message}"
                }
                _state.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }
}
