package com.nexory.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexory.app.data.local.TokenManager
import com.nexory.app.data.network.EventDto
import com.nexory.app.data.network.NexoryApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    // Лента "Все мероприятия" (фильтры на сервере)
    val upcoming:    List<EventDto> = emptyList(),
    val past:        List<EventDto> = emptyList(),
    // Лента "Мои записи" (поиск — на клиенте)
    val myUpcoming:  List<EventDto> = emptyList(),
    val myPast:      List<EventDto> = emptyList(),
    val isLoading:   Boolean        = false,
    val isMyEvents:  Boolean        = false,   // активная вкладка (0=Все, 1=Мои)
    val searchQuery: String         = "",
    val category:    String?        = null,
    val location:    String         = "",
    val sort:        String         = "soon",
    val freeOnly:    Boolean        = false,
    val maxPrice:    Int?           = null,
    val myUserId:    String?        = null,
    val error:       String?        = null,
) {
    val activeFilterCount: Int
        get() = listOf(location.isNotBlank(), sort != "soon", freeOnly, maxPrice != null).count { it }
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val api: NexoryApi,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    private var allJob: Job? = null
    private var searchJob: Job? = null
    private var myRaw: List<EventDto> = emptyList()   // мои события до клиентского поиска
    private var lastRefresh = 0L

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(myUserId = tokenManager.getUserId()) }
        }
        refresh()   // первичная загрузка сразу, не дожидаясь RESUMED
    }

    // Обновляем обе ленты (вызывается на RESUMED). Троттлинг убирает лишние
    // перезагрузки при быстром возврате на экран — меньше фризов.
    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh < 1500L) return
        lastRefresh = now
        loadAll()
        loadMy()
    }

    fun loadAll() {
        allJob?.cancel()
        allJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                val feed = api.getEvents(
                    category = state.category,
                    search   = state.searchQuery.takeIf { it.isNotBlank() },
                    location = state.location.takeIf { it.isNotBlank() },
                    sort     = state.sort,
                    freeOnly = if (state.freeOnly) "true" else null,
                    maxPrice = state.maxPrice?.toString(),
                )
                _uiState.update { it.copy(isLoading = false, upcoming = feed.upcoming, past = feed.past) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMy() {
        viewModelScope.launch {
            try {
                myRaw = api.getMyEvents()["events"] ?: emptyList()
                applyMyFilter()
            } catch (_: Exception) {}
        }
    }

    // Клиентский поиск по моим записям + разбивка на предстоящие/прошедшие
    private fun applyMyFilter() {
        val q = _uiState.value.searchQuery.trim()
        val filtered = if (q.isBlank()) myRaw else myRaw.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.address.contains(q, ignoreCase = true) ||
            (it.category?.contains(q, ignoreCase = true) == true)
        }
        _uiState.update {
            it.copy(
                myUpcoming = filtered.filterNot { e -> isPast(e.startsAt) },
                myPast     = filtered.filter { e -> isPast(e.startsAt) },
            )
        }
    }

    // Переключение вкладки (через свайп/тап)
    fun setMyEvents(my: Boolean) {
        if (_uiState.value.isMyEvents == my) return
        _uiState.update { it.copy(isMyEvents = my) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyMyFilter()  // мои записи фильтруем мгновенно
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            loadAll()    // все мероприятия — с дебаунсом на сервере
        }
    }

    fun setCategory(category: String?) { _uiState.update { it.copy(category = category) }; loadAll() }
    fun setLocation(location: String) { _uiState.update { it.copy(location = location) }; debouncedAll() }
    fun setSort(sort: String) { _uiState.update { it.copy(sort = sort) }; loadAll() }
    fun setFreeOnly(free: Boolean) { _uiState.update { it.copy(freeOnly = free) }; loadAll() }
    fun setMaxPrice(maxPrice: Int?) { _uiState.update { it.copy(maxPrice = maxPrice) }; debouncedAll() }

    fun resetFilters() {
        _uiState.update { it.copy(location = "", sort = "soon", freeOnly = false, maxPrice = null, category = null) }
        loadAll()
    }

    private fun debouncedAll() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(400); loadAll() }
    }

    private fun isPast(startsAt: String): Boolean = try {
        java.time.OffsetDateTime.parse(startsAt).toInstant().isBefore(java.time.Instant.now())
    } catch (_: Exception) { false }
}
