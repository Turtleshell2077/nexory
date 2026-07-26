package com.nexory.app.ui.screens.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.nexory.app.data.network.FriendDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.NexoryBottomBar
import com.nexory.app.ui.components.UserAvatar
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.theme.NexoryColors
import kotlinx.coroutines.launch

/** Единый горизонтальный отступ контента — раньше он разъезжался между строкой поиска и списком. */
private val ContentPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    // Две вкладки: Друзья и Запросы. Отдельной вкладки «Поиск» больше нет —
    // поиск живёт инлайн на вкладке «Друзья».
    val pagerState = rememberPagerState(initialPage = state.tab) { 2 }

    LaunchedEffect(pagerState.currentPage) { viewModel.setTab(pagerState.currentPage) }

    // Обновляем друзей/запросы при возвращении на экран (входящие заявки появятся)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load()
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Друзья", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        },
        bottomBar = { NexoryBottomBar(navController, Screen.Friends.route) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = NexoryColors.SurfaceDark,
                contentColor = NexoryColors.PrimaryBlue,
            ) {
                listOf(
                    "Друзья",
                    "Запросы${if (state.requests.isNotEmpty()) " (${state.requests.size})" else ""}",
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    ) {
                        Text(
                            title,
                            modifier = Modifier.padding(vertical = 12.dp),
                            fontSize = 14.sp,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> FriendsTab(
                        state = state,
                        onToggleSearch = viewModel::toggleSearch,
                        onSearch = viewModel::search,
                        onAdd = viewModel::sendRequest,
                        onChat = { friend ->
                            viewModel.openDirectChat(friend.id) { chatId ->
                                navController.navigate(Screen.ChatDetail.route(chatId))
                            }
                        },
                        // Полный профиль доступен только для тех, кто уже друг
                        onOpenFriendProfile = { navController.navigate(Screen.UserProfile.route(it.id)) },
                        onRemove = viewModel::removeFriend,
                    )
                    1 -> FriendRequestsList(
                        requests = state.requests,
                        onAccept = viewModel::acceptRequest,
                    )
                }
            }
        }
    }
}

/**
 * Вкладка «Друзья».
 *
 * Структура сверху вниз:
 *  1. Превью найденного по нику профиля (появляется только при вводе) — с кнопкой
 *     «Добавить в друзья». Перехода в полный профиль отсюда НЕТ: незнакомый человек
 *     не должен быть просматриваемым до того, как станет другом.
 *  2. Строка с кнопкой «Добавить друга», которая раскрывается в поле поиска по нику.
 *  3. Список друзей.
 */
@Composable
private fun FriendsTab(
    state: FriendsUiState,
    onToggleSearch: () -> Unit,
    onSearch: (String) -> Unit,
    onAdd: (String) -> Unit,
    onChat: (FriendDto) -> Unit,
    onOpenFriendProfile: (FriendDto) -> Unit,
    onRemove: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Фокус ставим ТОЛЬКО в момент, когда пользователь сам раскрыл поиск.
    //
    // Раньше эффект зависел от state.searchOpen: при возврате на вкладку «Друзья»
    // (поиск оставался раскрытым) он срабатывал заново и без спроса поднимал
    // клавиатуру. Флаг сбрасывается при закрытии поиска, поэтому повторное
    // открытие снова даёт фокус — как и ожидается.
    var focusConsumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.searchOpen) {
        if (state.searchOpen && !focusConsumed) {
            focusConsumed = true
            runCatching { focusRequester.requestFocus() }
        } else if (!state.searchOpen) {
            focusConsumed = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ---- 1. Превью найденного профиля (над строкой поиска) ----
        AnimatedVisibility(
            visible = state.searchOpen && (state.searchResults.isNotEmpty() || state.searchEmpty),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentPadding)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.searchEmpty) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoryColors.SurfaceDark)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.SearchOff, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Никого не найдено", color = NexoryColors.TextPrimary, fontSize = 14.sp)
                            Text("Проверьте написание ника", color = NexoryColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Ограничиваем количество превью: ник уникален, поэтому точное
                    // совпадение идёт первым (сортировка на бэкенде)
                    state.searchResults.take(5).forEach { user ->
                        FoundUserPreview(
                            user = user,
                            isSelf = user.id == state.myUserId,
                            isFriend = state.friends.any { it.id == user.id },
                            isRequestSent = user.id in state.sentRequests,
                            onAdd = { onAdd(user.id) },
                        )
                    }
                }
            }
        }

        // ---- 2. Кнопка «Добавить друга» / инлайн-строка поиска ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.searchOpen) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Введите ник", color = NexoryColors.TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = NexoryColors.TextSecondary) },
                    trailingIcon = {
                        when {
                            state.isSearching -> CircularProgressIndicator(
                                color = NexoryColors.PrimaryBlue,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            // Крестик очищает текст, но НЕ закрывает поиск —
                            // закрытие висит на отдельной кнопке справа от поля
                            state.searchQuery.isNotEmpty() -> IconButton(onClick = { onSearch("") }) {
                                Icon(
                                    Icons.Default.Cancel, "Очистить",
                                    tint = NexoryColors.TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                Spacer(Modifier.width(8.dp))
                // Свернуть поиск
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NexoryColors.SurfaceMid)
                        .clickable(onClick = onToggleSearch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Закрыть поиск", tint = NexoryColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
            } else {
                Button(
                    onClick = onToggleSearch,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить друга", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }

        // Подсказка про поиск только по нику
        if (state.searchOpen) {
            Text(
                "Поиск работает по нику — например, ivan_2007",
                color = NexoryColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = ContentPadding).padding(bottom = 8.dp),
            )
        }

        HorizontalDivider(color = NexoryColors.SurfaceMid)

        // ---- 3. Список друзей ----
        FriendsList(
            friends = state.friends,
            onChat = onChat,
            onProfile = onOpenFriendProfile,
            onRemove = onRemove,
        )
    }
}

/**
 * Превью найденного пользователя: аватар, имя, ник и действие.
 * Карточка НЕ кликабельна целиком — из превью нельзя открыть полный профиль.
 */
@Composable
private fun FoundUserPreview(
    user: FriendDto,
    isSelf: Boolean,
    isFriend: Boolean,
    isRequestSent: Boolean,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NexoryColors.SurfaceDark)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                url = user.avatarUrl,
                name = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                seed = user.id,
                size = 48.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                    color = NexoryColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text("@${user.username}", color = NexoryColors.TextSecondary, fontSize = 13.sp, maxLines = 1)
                if (!user.city.isNullOrBlank()) {
                    Text(user.city, color = NexoryColors.TextSecondary, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            isSelf -> PreviewStatus("Это ваш профиль", Icons.Default.Person)
            isFriend -> PreviewStatus("Уже у вас в друзьях", Icons.Default.Check)
            isRequestSent -> PreviewStatus("Заявка отправлена", Icons.Default.HourglassEmpty)
            else -> Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Добавить в друзья", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PreviewStatus(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NexoryColors.SurfaceMid)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = NexoryColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FriendsList(
    friends: List<FriendDto>,
    onChat: (FriendDto) -> Unit,
    onProfile: (FriendDto) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (friends.isEmpty()) {
        FriendsEmptyState(Icons.Default.PersonOff, "Пока нет друзей")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(friends, key = { it.id }) { friend ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NexoryColors.SurfaceDark),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        url = friend.avatarUrl,
                        name = friend.displayName?.takeIf { it.isNotBlank() } ?: friend.username,
                        seed = friend.id,
                        size = 46.dp,
                        modifier = Modifier.clickable { onProfile(friend) },
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).clickable { onProfile(friend) }) {
                        Text(
                            friend.displayName?.takeIf { it.isNotBlank() } ?: friend.username,
                            fontWeight = FontWeight.SemiBold,
                            color = NexoryColors.TextPrimary,
                            maxLines = 1,
                        )
                        Text("@${friend.username}", fontSize = 12.sp, color = NexoryColors.TextSecondary, maxLines = 1)
                    }
                    IconButton(onClick = { onChat(friend) }) {
                        Icon(Icons.Default.Chat, "Написать", tint = NexoryColors.PrimaryBlue)
                    }
                    IconButton(onClick = { onRemove(friend.id) }) {
                        Icon(Icons.Default.PersonRemove, "Удалить из друзей", tint = NexoryColors.Error)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRequestsList(requests: List<FriendDto>, onAccept: (String) -> Unit) {
    if (requests.isEmpty()) {
        FriendsEmptyState(Icons.Default.Inbox, "Нет новых запросов")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(requests, key = { it.id }) { req ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NexoryColors.SurfaceDark),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        url = req.avatarUrl,
                        name = req.displayName?.takeIf { it.isNotBlank() } ?: req.username,
                        seed = req.id,
                        size = 46.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            req.displayName?.takeIf { it.isNotBlank() } ?: req.username,
                            fontWeight = FontWeight.SemiBold,
                            color = NexoryColors.TextPrimary,
                            maxLines = 1,
                        )
                        Text("@${req.username}", fontSize = 12.sp, color = NexoryColors.TextSecondary, maxLines = 1)
                    }
                    Button(
                        onClick = { onAccept(req.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.DeepBlue),
                    ) {
                        Text("Принять", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsEmptyState(icon: ImageVector, text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text(text, color = NexoryColors.TextSecondary, fontSize = 16.sp)
        }
    }
}
