package com.nexory.app.ui.screens.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.EventDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.NexoryBottomBar
import com.nexory.app.ui.components.MetroAutocompleteField
import com.nexory.app.ui.components.scrollOnFocus
import com.nexory.app.ui.screens.profile.INTERESTS
import com.nexory.app.ui.screens.events.EVENT_CATEGORIES
import com.nexory.app.ui.screens.events.SKILL_LEVELS
import com.nexory.app.ui.screens.events.formatEventDateTime
import com.nexory.app.ui.screens.events.formatPrice
import com.nexory.app.ui.theme.NexoryColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Пейджер из 2 вкладок: 0 = Все мероприятия, 1 = Мои записи (плавный свайп)
    val pagerState = rememberPagerState(initialPage = if (uiState.isMyEvents) 1 else 0) { 2 }
    LaunchedEffect(pagerState.currentPage) { viewModel.setMyEvents(pagerState.currentPage == 1) }

    // Обновляем обе ленты при возвращении на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
            // Держим «Мои увлечения» в фильтре синхронными с профилем
            viewModel.syncMyInterests()
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .alpha(0.82f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                    )
                    .clickable { navController.navigate(Screen.CreateEvent.route) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать мероприятие", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        },
        bottomBar = { NexoryBottomBar(navController, currentRoute = Screen.Feed.route) }
    ) { padding ->
        if (showFilters) {
            // skipPartiallyExpanded — шторка сразу раскрывается на полный экран.
            // В половинном состоянии прокручивать было почти некуда: поле,
            // получившее фокус, упиралось в клавиатуру и оставалось за кадром,
            // сколько бы scrollOnFocus ни просил его показать.
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor   = NexoryColors.SurfaceDark,
            ) {
                // imePadding обязателен: внутри шторки есть поля ввода (цена, метро,
                // место, поиск увлечения). Без него клавиатура перекрывала поле,
                // и scrollOnFocus не мог подвести его в видимую область —
                // прокручивать было просто некуда.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    // Заголовок и сброс — в одной строке сверху: сбросить фильтры нужно
                    // быстро, а не долистывая весь список настроек до низа.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Фильтры",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = NexoryColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        val hasFilters = uiState.activeFilterCount > 0
                        TextButton(
                            onClick = { viewModel.resetFilters() },
                            enabled = hasFilters,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Default.Refresh, null,
                                tint = if (hasFilters) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Сбросить",
                                color = if (hasFilters) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Сортировка", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    // Раньше это были два чипа в Row с длинными подписями
                    // («Сначала ближайшие по дате» / «Сначала недавно созданные»):
                    // в строку они не помещались, второй уезжал за край экрана.
                    // Сегменты делят ширину поровну — подписи всегда выровнены,
                    // а разницу между вариантами объясняет строка под ними.
                    SegmentedSelector(
                        options       = listOf("Сначала ближайшие", "Сначала новые"),
                        selectedIndex = if (uiState.sort == "new") 1 else 0,
                        onSelect      = { viewModel.setSort(if (it == 1) "new" else "soon") },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (uiState.sort == "new") "Сверху — недавно опубликованные мероприятия"
                        else "Сверху — те, что состоятся раньше",
                        color = NexoryColors.TextSecondary, fontSize = 12.sp,
                    )

                    // Цена одним элементом: отдельный переключатель «Только бесплатные»
                    // убран — он дублировал крайнее левое положение шкалы.
                    Spacer(Modifier.height(16.dp))
                    PriceFilter(
                        maxPrice = uiState.maxPrice,
                        onChange = { viewModel.setMaxPrice(it) },
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Категория", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryChip("Все", uiState.category == null) { viewModel.setCategory(null) }
                        EVENT_CATEGORIES.forEach { cat ->
                            CategoryChip(cat, uiState.category == cat) { viewModel.setCategory(if (uiState.category == cat) null else cat) }
                        }
                    }

                    // ---- Увлечения: две равноправные вкладки ----
                    // Раньше личные увлечения были вложенной раскрывающейся панелью
                    // внутри общего списка — иерархия сбивала с толку. Теперь это
                    // два самостоятельных раздела на одном уровне.
                    Spacer(Modifier.height(18.dp))
                    InterestsFilterSection(
                        allInterests = INTERESTS,
                        myInterests = uiState.myInterests,
                        selected = uiState.selectedInterests,
                        onToggle = { viewModel.toggleInterest(it) },
                        onSelectAllMine = { viewModel.useMyProfileInterests() },
                        onClear = { viewModel.clearInterests() },
                        onAddCustom = { viewModel.addInterest(it) },
                    )


                    Spacer(Modifier.height(16.dp))
                    Text("Категория профессионализма", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SKILL_LEVELS.forEach { lvl ->
                            // "Любой уровень" = без фильтра (null)
                            val value = if (lvl == "Любой уровень") null else lvl
                            CategoryChip(lvl, uiState.level == value) { viewModel.setLevel(value) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Метро рядом", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    MetroAutocompleteField(value = uiState.metro, onChange = viewModel::setMetro)

                    Spacer(Modifier.height(16.dp))
                    Text("Место проведения", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    LocationFilter(value = uiState.location, onChange = viewModel::setLocation)

                    Spacer(Modifier.height(20.dp))
                    // «Сбросить» переехал в шапку — здесь остаётся только подтверждение
                    Button(
                        onClick = { showFilters = false },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                    ) { Text("Показать результаты", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Нет сети — честно говорим, что данные сохранённые, и когда обновлялись
            com.nexory.app.ui.components.OfflineBanner(
                visible = uiState.isOffline || uiState.isFromCache,
                cachedAt = uiState.cachedAt,
            )

            // Строка поиска + кнопка фильтров (фильтр — только на вкладке «Все»)
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FeedSearchBar(query = uiState.searchQuery, onSearch = viewModel::search)
                }
                if (pagerState.currentPage == 0) {
                    val active = uiState.activeFilterCount > 0
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) NexoryColors.PrimaryBlue else NexoryColors.SurfaceMid)
                            .clickable { showFilters = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Tune, "Фильтры", tint = if (active) Color.White else NexoryColors.TextSecondary)
                        if (active) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(8.dp)
                                    .background(Color.White, CircleShape),
                            )
                        }
                    }
                }
            }
            FeedToggle(
                isMyEvents = pagerState.currentPage == 1,
                onToggle   = { scope.launch { pagerState.animateScrollToPage(1 - pagerState.currentPage) } },
            )

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                if (page == 0) {
                    FeedPage(
                        upcoming = uiState.upcoming, past = uiState.past, isLoading = uiState.isLoading,
                        showCategoryBar = true, category = uiState.category, onSelectCategory = viewModel::setCategory,
                        markOwner = true, myUserId = uiState.myUserId, emptyIsMy = false,
                        onOpenEvent = { navController.navigate(Screen.EventDetail.route(it)) },
                    )
                } else {
                    FeedPage(
                        upcoming = uiState.myUpcoming, past = uiState.myPast, isLoading = uiState.isLoading,
                        showCategoryBar = false, category = null, onSelectCategory = {},
                        markOwner = true, myUserId = uiState.myUserId, emptyIsMy = true,
                        onOpenEvent = { navController.navigate(Screen.EventDetail.route(it)) },
                    )
                }
            }
        }
    }
}

// Одна страница ленты со своим скроллом и (для «Все») сворачивающейся панелью категорий
@Composable
private fun FeedPage(
    upcoming: List<EventDto>,
    past: List<EventDto>,
    isLoading: Boolean,
    showCategoryBar: Boolean,
    category: String?,
    onSelectCategory: (String?) -> Unit,
    markOwner: Boolean,
    myUserId: String?,
    emptyIsMy: Boolean,
    onOpenEvent: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    // Панель категорий: скрываем при прокрутке вниз, показываем при прокрутке вверх.
    //
    // ВАЖНО про «подскок» в конце списка: панель — сосед LazyColumn внутри Column,
    // поэтому её показ/скрытие меняет доступную списку высоту. Если список уже
    // долистан до конца, изменение высоты вынуждает его сдвинуть содержимое —
    // визуально это и есть «отскок». Поэтому у нижнего края состояние панели
    // замораживаем: у самого конца списка она не переключается.
    var barVisible by remember { mutableStateOf(true) }
    if (showCategoryBar) {
        LaunchedEffect(listState) {
            var li = listState.firstVisibleItemIndex
            var lo = listState.firstVisibleItemScrollOffset
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { (idx, off) ->
                    // Долистали до конца — не трогаем панель, иначе список дёрнется
                    if (!listState.canScrollForward) {
                        li = idx; lo = off
                        return@collect
                    }
                    barVisible = when {
                        idx == 0 && off == 0 -> true
                        idx < li -> true
                        idx > li -> false
                        off < lo -> true
                        off > lo -> false
                        else -> barVisible
                    }
                    li = idx; lo = off
                }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (showCategoryBar) {
            AnimatedVisibility(visible = barVisible) {
                CategoryFilter(selected = category, onSelect = onSelectCategory)
            }
        }

        val isEmpty = upcoming.isEmpty() && past.isEmpty() && !isLoading
        if (isEmpty) {
            EmptyFeed(isMyEvents = emptyIsMy)
        } else {
            LazyColumn(
                state = listState,
                // Нижний отступ увеличен намеренно: поверх ленты висит плавающая
                // кнопка «+» (56.dp + отступы) — при маленьком padding она перекрывала
                // низ последней карточки, где показаны дата и время мероприятия.
                // Плюс запас под жестовую навигационную полоску.
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(upcoming, key = { it.id }, contentType = { "event" }) { event ->
                    EventCard(
                        event      = event,
                        ownerBadge = markOwner && event.creatorId != null && event.creatorId == myUserId,
                        onClick    = { onOpenEvent(event.id) },
                    )
                }
                if (past.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NexoryColors.SurfaceMid)
                            Text("  Прошедшие мероприятия  ", color = NexoryColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NexoryColors.SurfaceMid)
                        }
                    }
                    items(past, key = { "past_${it.id}" }, contentType = { "event" }) { event ->
                        Box(modifier = Modifier.alpha(0.55f)) {
                            EventCard(
                                event      = event,
                                ownerBadge = markOwner && event.creatorId != null && event.creatorId == myUserId,
                                onClick    = { onOpenEvent(event.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeed(isMyEvents: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EventBusy, contentDescription = null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                if (isMyEvents) "Ты ещё не записался на мероприятия" else "Мероприятий пока нет",
                color = NexoryColors.TextSecondary,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun FeedSearchBar(query: String, onSearch: (String) -> Unit) {
    var text by remember { mutableStateOf(query) }
    OutlinedTextField(
        value         = text,
        onValueChange = { text = it; onSearch(it) },
        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder   = { Text("Поиск мероприятий...", color = NexoryColors.TextSecondary) },
        leadingIcon   = { Icon(Icons.Default.Search, null, tint = NexoryColors.TextSecondary) },
        trailingIcon  = if (text.isNotEmpty()) {{
            IconButton(onClick = { text = ""; onSearch("") }) {
                Icon(Icons.Default.Clear, null, tint = NexoryColors.TextSecondary)
            }
        }} else null,
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = NexoryColors.PrimaryBlue,
            unfocusedBorderColor    = NexoryColors.SurfaceMid,
            focusedContainerColor   = NexoryColors.SurfaceMid,
            unfocusedContainerColor = NexoryColors.SurfaceMid,
            cursorColor             = NexoryColors.PrimaryBlue,
            focusedTextColor        = NexoryColors.TextPrimary,
            unfocusedTextColor      = NexoryColors.TextPrimary,
        )
    )
}

@Composable
private fun LocationFilter(value: String, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value         = text,
        onValueChange = { text = it; onChange(it) },
        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp).scrollOnFocus(),
        placeholder   = { Text("Место (город, район)...", color = NexoryColors.TextSecondary) },
        leadingIcon   = { Icon(Icons.Default.LocationOn, null, tint = NexoryColors.TextSecondary) },
        trailingIcon  = if (text.isNotEmpty()) {{
            IconButton(onClick = { text = ""; onChange("") }) {
                Icon(Icons.Default.Clear, null, tint = NexoryColors.TextSecondary)
            }
        }} else null,
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = NexoryColors.PrimaryBlue,
            unfocusedBorderColor    = NexoryColors.SurfaceMid,
            focusedContainerColor   = NexoryColors.SurfaceMid,
            unfocusedContainerColor = NexoryColors.SurfaceMid,
            cursorColor             = NexoryColors.PrimaryBlue,
            focusedTextColor        = NexoryColors.TextPrimary,
            unfocusedTextColor      = NexoryColors.TextPrimary,
        )
    )
}

@Composable
private fun CategoryFilter(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(label = "Все", active = selected == null) { onSelect(null) }
        EVENT_CATEGORIES.forEach { cat ->
            CategoryChip(label = cat, active = selected == cat) {
                onSelect(if (selected == cat) null else cat)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) NexoryColors.PrimaryBlue else NexoryColors.SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active) Color.White else NexoryColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Сегментированный переключатель на всю ширину.
 *
 * Сегменты делят ширину поровну, поэтому подписи выровнены между собой
 * независимо от длины текста — в отличие от чипов в Row, где длинные варианты
 * не помещались в строку и уезжали за край экрана.
 */
@Composable
private fun SegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexoryColors.SurfaceMid)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, title ->
            val active = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) NexoryColors.PrimaryBlue else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    color = if (active) Color.White else NexoryColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/**
 * Фильтр по увлечениям: две равноправные вкладки.
 *
 *  «Все увлечения» — пользователь вводит увлечение сам, подсказки появляются
 *                    по мере ввода; чего нет в подсказках — добавляется кнопкой «+».
 *  «Мои увлечения» — то, что указано в профиле, с кнопками выбрать всё / очистить.
 *
 * Готового списка-простыни здесь намеренно НЕТ. Раньше вкладка «Все увлечения»
 * вываливала два десятка чипов сразу: их приходилось вычитывать глазами, они
 * занимали пол-экрана шторки и всё равно не покрывали то, что человек ищет.
 * Ввод с автоподбором — ровно тот же приём, что и в редактировании профиля.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsFilterSection(
    allInterests: List<String>,
    myInterests: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAllMine: () -> Unit,
    onClear: () -> Unit,
    onAddCustom: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Увлечения",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f),
            )
            if (selected.isNotEmpty()) {
                Text(
                    "Выбрано: ${selected.size}",
                    color = NexoryColors.PrimaryBlue, fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        SegmentedSelector(
            options = listOf("Все увлечения", "Мои увлечения"),
            selectedIndex = tab,
            onSelect = { tab = it },
        )

        Spacer(Modifier.height(12.dp))

        if (tab == 0) {
            val suggestions = remember(query, selected) {
                val q = query.trim()
                if (q.isBlank()) emptyList()
                else allInterests.filter { it.contains(q, ignoreCase = true) && it !in selected }.take(6)
            }
            // Кнопки «+» рядом с полем нет: добавить своё значение предлагает
            // сам список подсказок последним пунктом «Добавить …», а лишний
            // элемент управления сбоку только сужал поле ввода.
            com.nexory.app.ui.components.AutocompleteTextField(
                value = query,
                onValueChange = { query = it },
                suggestions = suggestions,
                onSuggestionPick = { onToggle(it); query = "" },
                onCustomValueAdd = { onAddCustom(it); query = "" },
                placeholder = "Начните вводить увлечение",
                allowCustomValue = true,
            )

            Spacer(Modifier.height(10.dp))
            if (selected.isEmpty()) {
                Text(
                    "Подсказки появятся по мере ввода. Если нужного увлечения нет в списке — " +
                        "добавьте своё кнопкой «+».",
                    color = NexoryColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                )
            } else {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.TextSecondary),
                ) { Text("Очистить", fontSize = 13.sp) }
                Spacer(Modifier.height(10.dp))
                // Показываем только выбранное: общий список пользователь набирает сам
                FlowRowChips(items = selected.toList(), selected = selected, onToggle = onToggle)
            }
        } else {
            if (myInterests.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoryColors.SurfaceMid)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Interests, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Увлечения пока не указаны",
                        color = NexoryColors.TextPrimary, fontSize = 14.sp,
                    )
                    Text(
                        "Добавьте их в профиле — они появятся здесь",
                        color = NexoryColors.TextSecondary, fontSize = 12.sp,
                    )
                }
            } else {
                val allMineSelected = myInterests.all { it in selected }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSelectAllMine,
                        enabled = !allMineSelected,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
                    ) { Text("Выбрать все", fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = onClear,
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.TextSecondary),
                    ) { Text("Очистить", fontSize = 13.sp) }
                }
                Spacer(Modifier.height(10.dp))
                FlowRowChips(items = myInterests, selected = selected, onToggle = onToggle)
            }
        }
    }
}


/**
 * Фильтр по цене: слайдер, у которого значение можно ещё и ввести вручную.
 *
 * Почему так: тащить ползунок удобно для «примерно до 1000», но неудобно, когда
 * нужна точная сумма — поэтому по тапу на цифру открывается ввод с клавиатуры.
 * Крайнее левое положение (0 ₽) подписано как «Только бесплатные»: прежняя
 * отдельная кнопка делала ровно это и просто дублировала шкалу.
 *
 * Шкала нелинейная по шагу: до 1000 ₽ шаг 50, выше — 250. Мероприятия чаще
 * бесплатные или недорогие, и мелкий шаг важен именно в начале шкалы.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PriceFilter(maxPrice: Int?, onChange: (Int?) -> Unit) {
    val maxLimit = 10_000
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    // null = фильтр по цене не задан, показываем ползунок в крайнем правом положении
    val current = maxPrice ?: maxLimit
    val isFree = maxPrice == 0

    // Подводим к клавиатуре ВЕСЬ блок цены, а не только поле ввода: рядом
    // стоит шкала, и без неё введённая сумма ни с чем не соотносится.
    val blockRequester = com.nexory.app.ui.components.rememberBringIntoView(active = editing)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(blockRequester)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Цена",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f),
            )
            if (editing) {
                // Открыли ввод — сразу ставим фокус, иначе пользователю пришлось бы
                // тапать второй раз, чтобы вызвать клавиатуру
                val focusRequester = remember { FocusRequester() }
                // Поле ХОТЯ БЫ РАЗ получало фокус?
                //
                // Без этого флага ввод был полностью нерабочим: onFocusChanged
                // срабатывает уже в момент появления поля, со значением
                // isFocused = false. Проверка «фокус потерян → применить и
                // закрыть» тут же закрывала ввод — раньше, чем requestFocus()
                // успевал его открыть. Снаружи это выглядело так, будто в поле
                // просто нельзя ничего написать.
                var everFocused by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    runCatching { focusRequester.requestFocus() }
                }
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it.filter { c -> c.isDigit() }.take(5) },
                    // scrollOnFocus здесь НЕ нужен: прокруткой занимается
                    // blockRequester выше — он показывает поле вместе со шкалой
                    modifier = Modifier
                        .width(130.dp)
                        .focusRequester(focusRequester)
                        // Ушли с поля, не нажав «Готово» — сумму всё равно применяем,
                        // иначе введённое молча пропадает
                        .onFocusChanged { st ->
                            if (st.isFocused) {
                                everFocused = true
                            } else if (everFocused && editing) {
                                onChange(editText.toIntOrNull()?.coerceIn(0, maxLimit))
                                editing = false
                            }
                        },
                    singleLine = true,
                    placeholder = { Text("₽", color = NexoryColors.TextSecondary) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            onChange(editText.toIntOrNull()?.coerceIn(0, maxLimit))
                            editing = false
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = com.nexory.app.ui.components.nexoryTextFieldColors(),
                )
            } else {
                // Значение — кликабельное: тап открывает точный ввод
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexoryColors.SurfaceMid)
                        .clickable {
                            editText = maxPrice?.toString() ?: ""
                            editing = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when {
                            isFree -> "Только бесплатные"
                            maxPrice == null -> "Любая"
                            else -> "до $maxPrice ₽"
                        },
                        color = if (isFree) NexoryColors.PrimaryBlue else NexoryColors.TextPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Edit, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(13.dp))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Slider(
            value = current.toFloat(),
            onValueChange = { raw ->
                val v = raw.toInt()
                // Округляем к «человеческим» значениям, чтобы не получалось «до 337 ₽»
                val stepped = when {
                    v <= 0 -> 0
                    v < 1000 -> (v / 50) * 50
                    else -> (v / 250) * 250
                }
                onChange(if (stepped >= maxLimit) null else stepped)
            },
            valueRange = 0f..maxLimit.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = NexoryColors.PrimaryBlue,
                activeTrackColor = NexoryColors.PrimaryBlue,
                inactiveTrackColor = NexoryColors.SurfaceMid,
            ),
        )
        // Подписи по краям шкалы — не украшение, а кнопки: тап ставит крайнее
        // значение. Точно попасть ползунком в 0 или в самый конец шкалы неудобно,
        // а это два самых частых выбора.
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Только бесплатные",
                color = if (isFree) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isFree) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { editing = false; onChange(0) }
                    .padding(vertical = 4.dp),
            )
            val isAny = maxPrice == null
            Text(
                "Любая цена",
                color = if (isAny) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isAny) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { editing = false; onChange(null) }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

// Чипы увлечений в фильтре — выбранные подсвечены и имеют крестик
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(items: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val isSel = item in selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSel) NexoryColors.PrimaryBlue else NexoryColors.SurfaceMid)
                    .clickable { onToggle(item) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, color = if (isSel) Color.White else NexoryColors.TextSecondary, fontSize = 13.sp)
                if (isSel) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun EventCard(event: EventDto, ownerBadge: Boolean = false, onClick: () -> Unit) {
    // Отметки «Мероприятие завершено» здесь намеренно НЕТ: в ленте прошедшие
    // и так собраны под отдельным заголовком и приглушены, а бейдж на карточке
    // дублировал это и занимал строку. Отметка живёт на экране мероприятия.
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Рамка своего мероприятия — акцентная; у остальных берётся CardBorder.
            // В тёмной теме он прозрачный (карточка и так контрастна к фону),
            // в светлой — тонкая лавандовая линия, иначе белая карточка на
            // светлом фоне расплывается.
            .border(
                width = if (ownerBadge) 1.5.dp else 1.dp,
                color = if (ownerBadge) NexoryColors.PrimaryBlue.copy(alpha = 0.5f) else NexoryColors.CardBorder,
                shape = RoundedCornerShape(16.dp),
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = NexoryColors.SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            if (event.coverUrl != null) {
                AsyncImage(
                    model              = event.coverUrl,
                    contentDescription = event.title,
                    modifier           = Modifier.fillMaxWidth().height(160.dp),
                    contentScale       = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NexoryColors.DeepBlue.copy(alpha = 0.6f),
                                    NexoryColors.Violet.copy(alpha = 0.4f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Event, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                // Бейджи: организатор, цена, уровень
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    if (ownerBadge) {
                        Box(
                            modifier = Modifier
                                .background(NexoryColors.PrimaryBlue, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("Организатор", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    // Платное выделяем тёплым Accent2 — единственный не-фиолетовый
                    // цвет в палитре, поэтому цена сразу цепляет взгляд среди
                    // остальных бейджей
                    val free = event.price == null || event.price <= 0.0
                    val priceColor = if (free) NexoryColors.PrimaryBlue else NexoryColors.Accent2
                    Box(
                        modifier = Modifier
                            .background(priceColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(formatPrice(event.price), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = priceColor)
                    }
                    event.skillLevel?.takeIf { it.isNotBlank() }?.let {
                        Box(
                            modifier = Modifier
                                .background(NexoryColors.SurfaceMid, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(it, fontSize = 11.sp, color = NexoryColors.TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                event.category?.let {
                    Text(it.uppercase(), fontSize = 11.sp, color = NexoryColors.AccentText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(event.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(event.address, fontSize = 13.sp, color = NexoryColors.TextSecondary, maxLines = 1)
                }
                event.metro?.takeIf { it.isNotBlank() }?.let { m ->
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("м. $m", fontSize = 12.sp, color = NexoryColors.PrimaryBlue, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        // Дата + время начала и окончания
                        Text(formatEventDateTime(event.startsAt, event.endsAt), fontSize = 12.sp, color = NexoryColors.PrimaryBlue)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (event.maxParticipants != null) "${event.participantCount}/${event.maxParticipants}" else "${event.participantCount}",
                            fontSize = 12.sp,
                            color    = NexoryColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
