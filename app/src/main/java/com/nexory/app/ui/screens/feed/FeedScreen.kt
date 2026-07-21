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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                containerColor   = NexoryColors.SurfaceDark,
            ) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                    Text("Фильтры", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)

                    Spacer(Modifier.height(16.dp))
                    Text("Сортировка", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryChip("Сначала ближайшие", uiState.sort == "soon") { viewModel.setSort("soon") }
                        CategoryChip("Сначала новые", uiState.sort == "new") { viewModel.setSort("new") }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Только бесплатные", fontSize = 14.sp, color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = uiState.freeOnly,
                            onCheckedChange = { viewModel.setFreeOnly(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NexoryColors.PrimaryBlue,
                                uncheckedTrackColor = NexoryColors.SurfaceMid,
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Максимальная цена (₽)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    var maxPriceText by remember { mutableStateOf(uiState.maxPrice?.toString() ?: "") }
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = {
                            maxPriceText = it.filter { c -> c.isDigit() }
                            viewModel.setMaxPrice(maxPriceText.toIntOrNull())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("До какой цены готов пойти", color = NexoryColors.TextSecondary) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = NexoryColors.PrimaryBlue,
                            unfocusedBorderColor    = NexoryColors.SurfaceMid,
                            focusedContainerColor   = NexoryColors.SurfaceMid,
                            unfocusedContainerColor = NexoryColors.SurfaceMid,
                            cursorColor             = NexoryColors.PrimaryBlue,
                            focusedTextColor        = NexoryColors.TextPrimary,
                            unfocusedTextColor      = NexoryColors.TextPrimary,
                        ),
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

                    // Фильтр по увлечениям
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Увлечения", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f))
                        if (uiState.myInterests.isNotEmpty()) {
                            TextButton(onClick = { viewModel.useMyProfileInterests() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Icon(Icons.Default.Person, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Мои увлечения", color = NexoryColors.PrimaryBlue, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Поле ввода нового увлечения
                    var interestInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = interestInput,
                        onValueChange = { interestInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Например: футбол, музыка…", color = NexoryColors.TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Interests, null, tint = NexoryColors.TextSecondary) },
                        trailingIcon = {
                            if (interestInput.isNotBlank()) {
                                IconButton(onClick = { viewModel.addInterest(interestInput); interestInput = "" }) {
                                    Icon(Icons.Default.Add, "Добавить", tint = NexoryColors.PrimaryBlue)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = NexoryColors.PrimaryBlue,
                            unfocusedBorderColor    = NexoryColors.SurfaceMid,
                            focusedContainerColor   = NexoryColors.SurfaceMid,
                            unfocusedContainerColor = NexoryColors.SurfaceMid,
                            cursorColor             = NexoryColors.PrimaryBlue,
                            focusedTextColor        = NexoryColors.TextPrimary,
                            unfocusedTextColor      = NexoryColors.TextPrimary,
                        ),
                    )
                    // Подсказки из профиля — быстрый выбор тапом
                    val suggestions = (uiState.myInterests + uiState.selectedInterests.toList()).distinct()
                    if (suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRowChips(
                            items = suggestions,
                            selected = uiState.selectedInterests,
                            onToggle = { viewModel.toggleInterest(it) },
                        )
                    }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.resetFilters() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.TextSecondary),
                        ) { Text("Сбросить") }
                        Button(
                            onClick = { showFilters = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                        ) { Text("Показать") }
                    }
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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

    // Панель категорий: скрываем при прокрутке вниз, показываем при прокрутке вверх
    var barVisible by remember { mutableStateOf(true) }
    if (showCategoryBar) {
        LaunchedEffect(listState) {
            var li = listState.firstVisibleItemIndex
            var lo = listState.firstVisibleItemScrollOffset
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { (idx, off) ->
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
                state               = listState,
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (ownerBadge) Modifier.border(1.5.dp, NexoryColors.PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else Modifier),
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
                    val free = event.price == null || event.price <= 0.0
                    Box(
                        modifier = Modifier
                            .background(
                                (if (free) NexoryColors.PrimaryBlue else NexoryColors.Violet).copy(alpha = 0.18f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(formatPrice(event.price), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (free) NexoryColors.PrimaryBlue else NexoryColors.Violet)
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
                    Text(it.uppercase(), fontSize = 11.sp, color = NexoryColors.LightViolet, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
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
