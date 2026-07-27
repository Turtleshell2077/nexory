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
    val isLoading:     Boolean       = false,
    val isCreated:     Boolean       = false,
    val error:         String?       = null,
    val invalidFields: Set<String>   = emptySet(),  // имена полей с ошибкой (для подсветки)
    val coverError:    String?       = null,        // причина сбоя загрузки обложки
    val loaded:        EventDto?      = null,        // для режима редактирования
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val api: NexoryApi,
    private val uploader: MediaUploader,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEventUiState())
    val uiState = _state.asStateFlow()

    fun clearError() { _state.update { it.copy(error = null) } }

    // Сопоставление имён полей сервера с UI-ключами (для подсветки ячеек)
    private fun serverFieldToUi(field: String): String? = when (field) {
        "title"            -> "title"
        "address"          -> "address"
        "starts_at"        -> "date"
        "max_participants" -> "maxParticipants"
        "price"            -> "price"
        "description"      -> "description"
        else -> null
    }

    /**
     * Загрузка обложки. Возвращает URL либо null, а причину сбоя кладёт в state —
     * раньше ошибка молча терялась, и пользователь видел бесконечный спиннер.
     */
    suspend fun uploadImage(uri: Uri): String? {
        _state.update { it.copy(coverError = null) }
        return when (val result = uploader.uploadWithResult(uri)) {
            is com.nexory.app.data.network.UploadResult.Success -> result.url
            is com.nexory.app.data.network.UploadResult.Failure -> {
                _state.update { it.copy(coverError = result.message) }
                null
            }
        }
    }

    fun clearCoverError() { _state.update { it.copy(coverError = null) } }

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
        ticketUrl: String? = null,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, invalidFields = emptySet()) }
            try {
                val body = buildMap<String, String?> {
                    put("title",      title)
                    put("address",    address)
                    put("starts_at",  startsAt)
                    put("is_private", isPrivate.toString())
                    put("price",      (price ?: 0.0).toString())
                    if (!endsAt.isNullOrBlank())      put("ends_at",          endsAt)
                    if (maxParticipants != null)      put("max_participants", maxParticipants.toString())
                    // Пустая строка — это осознанное «удалить обложку», её нужно
                    // отправить; null означает «не менять» и не отправляется вовсе.
                    if (coverUrl != null)             put("cover_url",        coverUrl)
                    // Остальные текстовые поля тоже отправляем всегда — см. комментарий ниже
                    put("description",       description ?: "")
                    put("category",          category ?: "")
                    put("skill_level",       skillLevel ?: "")
                    put("event_type",        eventType ?: "")
                    put("price_description", priceDescription ?: "")
                    // Текстовые поля отправляем ВСЕГДА, включая пустую строку:
                    // на сервере '' означает «очистить», а отсутствие поля —
                    // «не менять». Раньше пустые значения не отправлялись,
                    // и очистить поле при редактировании было невозможно.
                    put("metro",             metro ?: "")
                    put("ticket_url",        ticketUrl ?: "")
                }
                // Берём мероприятие из ОТВЕТА сервера, а не полагаемся на локальные
                // значения формы: так экран и кэш получают ровно то, что реально
                // записано в БД (включая новую ссылку на обложку).
                val response = if (eventId == null) api.createEvent(body) else api.updateEvent(eventId, body)
                val saved = response["event"]
                _state.update { it.copy(isLoading = false, isCreated = true, loaded = saved ?: it.loaded) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                val parsed = com.nexory.app.data.network.ApiError.parse(e)
                // Переводим имена полей бэкенда в наши ключи для подсветки ячеек
                val fields = parsed.fields.mapNotNull { serverFieldToUi(it) }.toSet()
                _state.update { it.copy(isLoading = false, error = parsed.message, invalidFields = fields) }
            }
        }
    }
}
