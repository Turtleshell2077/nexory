package com.nexory.app.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.FriendDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.NexoryBottomBar
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.theme.NexoryColors
import androidx.compose.material3.ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
            TabRow(selectedTabIndex = state.tab, containerColor = NexoryColors.SurfaceDark, contentColor = NexoryColors.PrimaryBlue) {
                listOf(
                    "Друзья",
                    "Запросы${if (state.requests.isNotEmpty()) " (${state.requests.size})" else ""}",
                    "Поиск"
                ).forEachIndexed { index, title ->
                    Tab(selected = state.tab == index, onClick = { viewModel.setTab(index) }) {
                        Text(title, modifier = Modifier.padding(vertical = 12.dp), fontSize = 14.sp,
                            fontWeight = if (state.tab == index) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            when (state.tab) {
                0 -> FriendsList(
                    friends  = state.friends,
                    onChat   = { friend -> viewModel.openDirectChat(friend.id) { chatId -> navController.navigate(Screen.ChatDetail.route(chatId)) } },
                    onProfile = { navController.navigate(Screen.UserProfile.route(it.id)) },
                    onRemove = viewModel::removeFriend,
                )
                1 -> FriendRequestsList(requests = state.requests, onAccept = viewModel::acceptRequest)
                2 -> UserSearchTab(
                    query        = state.searchQuery,
                    results      = state.searchResults,
                    friends      = state.friends.map { it.id }.toSet(),
                    sentRequests = state.sentRequests,
                    myUserId     = state.myUserId,
                    onSearch     = viewModel::search,
                    onAdd        = viewModel::sendRequest,
                    onProfile    = { navController.navigate(Screen.UserProfile.route(it)) },
                )
            }
        }
    }
}

@Composable
fun FriendsList(friends: List<FriendDto>, onChat: (FriendDto) -> Unit, onProfile: (FriendDto) -> Unit, onRemove: (String) -> Unit) {
    if (friends.isEmpty()) { FriendsEmptyState(Icons.Default.PersonOff, "Пока нет друзей"); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(friends, key = { it.id }) { friend ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NexoryColors.SurfaceDark), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = friend.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(46.dp).clip(CircleShape).background(NexoryColors.SurfaceMid).clickable { onProfile(friend) })
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).clickable { onProfile(friend) }) {
                        Text(friend.displayName?.takeIf { it.isNotBlank() } ?: friend.username, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                        Text("@${friend.username}", fontSize = 12.sp, color = NexoryColors.TextSecondary)
                    }
                    IconButton(onClick = { onChat(friend) }) { Icon(Icons.Default.Chat, null, tint = NexoryColors.PrimaryBlue) }
                    IconButton(onClick = { onRemove(friend.id) }) { Icon(Icons.Default.PersonRemove, null, tint = NexoryColors.Error) }
                }
            }
        }
    }
}

@Composable
fun FriendRequestsList(requests: List<FriendDto>, onAccept: (String) -> Unit) {
    if (requests.isEmpty()) { FriendsEmptyState(Icons.Default.Inbox, "Нет новых запросов"); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(requests, key = { it.id }) { req ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NexoryColors.SurfaceDark), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = req.avatarUrl, contentDescription = null, modifier = Modifier.size(46.dp).clip(CircleShape).background(NexoryColors.SurfaceMid))
                    Spacer(Modifier.width(12.dp))
                    Text(req.username, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f))
                    Button(onClick = { onAccept(req.id) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.DeepBlue)) {
                        Text("Принять", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchTab(query: String, results: List<FriendDto>, friends: Set<String>, sentRequests: Set<String>, myUserId: String?, onSearch: (String) -> Unit, onAdd: (String) -> Unit, onProfile: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = query, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("@ник или имя", color = NexoryColors.TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = NexoryColors.TextSecondary) },
            singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoryTextFieldColors())
        Text(
            "Ищите по @нику (например, @ivan) или по имени",
            color = NexoryColors.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.id }) { user ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NexoryColors.SurfaceDark),
                    modifier = Modifier.fillMaxWidth().clickable { onProfile(user.id) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(NexoryColors.SurfaceMid))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName?.takeIf { it.isNotBlank() } ?: user.username, color = NexoryColors.TextPrimary, fontWeight = FontWeight.Medium)
                            Text("@${user.username}", color = NexoryColors.TextSecondary, fontSize = 12.sp)
                        }
                        when {
                            user.id == myUserId -> Text("Это вы", color = NexoryColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                            user.id in friends -> Icon(Icons.Default.Check, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.padding(8.dp))
                            user.id in sentRequests -> Icon(Icons.Default.HourglassEmpty, null, tint = NexoryColors.TextSecondary, modifier = Modifier.padding(8.dp))
                            else -> Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NexoryColors.PrimaryBlue)
                                    .clickable { onAdd(user.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.PersonAdd, "Добавить в друзья", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
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