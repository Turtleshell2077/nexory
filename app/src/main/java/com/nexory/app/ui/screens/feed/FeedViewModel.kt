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
    val level:       String?        = null,   // категория профессионализма
    val metro:       String         = "",     // ближайшее метро
    val selectedInterests: Set<String> = emptySet(), // выбранные увлечения для фильтра
    val myInterests: List<String>   = emptyList(),    // увлечения из профиля (для кнопки «вставить»)
    val myUserId:    String?        = null,
    val error:       String?        = null,
) {
    val activeFilterCount: Int
        get() = listOf(
            location.isNotBlank(), sort != "soon", freeOnly, maxPrice != null,
            level != null, metro.isNotBlank(), selectedInterests.isNotEmpty(),
        ).count { it }
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
            // Подтягиваем любимые категории (виды спорта) из профиля для фильтра "по интересам"
            try {
                val sports = api.getMyProfile().user?.sports
                val interests = sports?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                _uiState.update { it.copy(myInterests = interests) }
            } catch (_: Exception) {}
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
                    level    = state.level,
                    metro    = state.metro.takeIf { it.isNotBlank() },
                )
                val upcoming = applyInterestFilter(feed.upcoming)
                val past     = applyInterestFilter(feed.past)
                _uiState.update { it.copy(isLoading = false, upcoming = upcoming, past = past) }
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
    fun setLevel(level: String?) { _uiState.update { it.copy(level = level) }; loadAll() }
    fun setMetro(metro: String) { _uiState.update { it.copy(metro = metro) }; debouncedAll() }

    // Увлечения-фильтр
    fun toggleInterest(interest: String) {
        val i = interest.trim()
        if (i.isBlank()) return
        _uiState.update {
            val set = it.selectedInterests.toMutableSet()
            if (!set.add(i)) set.remove(i)
            it.copy(selectedInterests = set)
        }
        loadAll()
    }
    fun addInterest(interest: String) {
        val i = interest.trim()
        if (i.isBlank()) return
        _uiState.update { it.copy(selectedInterests = it.selectedInterests + i) }
        loadAll()
    }
    // Скопировать все увлечения из профиля в фильтр
    fun useMyProfileInterests() {
        _uiState.update { it.copy(selectedInterests = it.selectedInterests + it.myInterests) }
        loadAll()
    }
    fun clearInterests() { _uiState.update { it.copy(selectedInterests = emptySet()) }; loadAll() }

    fun resetFilters() {
        _uiState.update { it.copy(
            location = "", sort = "soon", freeOnly = false, maxPrice = null, category = null,
            level = null, metro = "", selectedInterests = emptySet(),
        ) }
        loadAll()
    }

    // Клиентский фильтр по увлечениям: оставляем события, чья категория/название/описание
    // совпадает хотя бы с одним выбранным увлечением. Сервер про увлечения не знает.
    private fun applyInterestFilter(events: List<EventDto>): List<EventDto> {
        val st = _uiState.value
        if (st.selectedInterests.isEmpty()) return events
        return events.filter { e ->
            st.selectedInterests.any { interest ->
                val i = interest.lowercase()
                (e.category?.lowercase()?.contains(i) == true) ||
                e.title.lowercase().contains(i) ||
                (e.description?.lowercase()?.contains(i) == true)
            }
        }
    }

    private fun debouncedAll() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(400); loadAll() }
    }

    private fun isPast(startsAt: String): Boolean = try {
        java.time.OffsetDateTime.parse(startsAt).toInstant().isBefore(java.time.Instant.now())
    } catch (_: Exception) { false }
}
