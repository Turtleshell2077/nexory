package com.nexory.app.ui.screens.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nexory.app.data.network.ChatDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.NexoryBottomBar
import com.nexory.app.ui.theme.NexoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    navController: NavController,
    viewModel: ChatsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    // Обновляем список при возвращении на экран (но не в режиме выбора)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!state.selectionMode) viewModel.load(archived = false)
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    title = { Text("Выбрано: ${state.selected.size}", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelection() }) {
                            Icon(Icons.Default.Close, null, tint = NexoryColors.TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.archiveSelected() }, enabled = state.selected.isNotEmpty()) {
                            Icon(Icons.Default.Archive, "В архив", tint = NexoryColors.TextPrimary)
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }, enabled = state.selected.isNotEmpty()) {
                            Icon(Icons.Default.Delete, "Удалить", tint = NexoryColors.Error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
                )
            } else {
                TopAppBar(
                    title = { Text("Чаты", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, "Меню", tint = NexoryColors.TextPrimary)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Выбрать чаты") },
                                onClick = { menuOpen = false; viewModel.enterSelection() },
                                leadingIcon = { Icon(Icons.Default.Checklist, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Создать чат") },
                                onClick = { menuOpen = false; navController.navigate(Screen.NewChat.route) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Открыть архив") },
                                onClick = { menuOpen = false; navController.navigate(Screen.Archive.route) },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
                )
            }
        },
        bottomBar = { NexoryBottomBar(navController, Screen.Chats.route) }
    ) { padding ->
        if (state.chats.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Нет чатов", color = NexoryColors.TextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { navController.navigate(Screen.NewChat.route) }) {
                        Text("Создать чат", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(state.chats, key = { it.id }) { chat ->
                ChatListItem(
                    chat          = chat,
                    selectionMode = state.selectionMode,
                    selected      = chat.id in state.selected,
                    onClick       = {
                        if (state.selectionMode) viewModel.toggleSelect(chat.id)
                        else navController.navigate(Screen.ChatDetail.route(chat.id))
                    },
                    onLongPress   = { viewModel.enterSelection(chat.id) },
                    onMute        = { viewModel.setMuted(chat.id, true) },
                    onArchive     = { viewModel.archive(chat.id) },
                    onDelete      = { viewModel.deleteChat(chat.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: ChatDto,
    onClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPress: () -> Unit = {},
    onMute: () -> Unit = {},
    onArchive: () -> Unit = {},
    onDelete: () -> Unit = {},
    archiveLabel: String = "В архив",
) {
    val title    = when (chat.type) { "direct" -> chat.peer?.username ?: "Чат"; "event" -> chat.eventInfo?.title ?: "Чат мероприятия"; "support" -> "Поддержка"; else -> "Чат" }
    val subtitle = chat.lastMessage?.content ?: "Нет сообщений"
    val time     = chat.lastMessage?.createdAt?.let { if (it.length >= 16) it.substring(11, 16) else "" } ?: ""
    var menuOpen by remember { mutableStateOf(false) }

    Box {
    Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) NexoryColors.PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = { if (!selectionMode) menuOpen = true })
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null, tint = if (selected) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NexoryColors.DeepBlue.copy(0.6f), NexoryColors.Violet.copy(0.6f)))), contentAlignment = Alignment.Center) {
            val avatar = chat.avatarUrl ?: chat.peer?.avatarUrl
            if (avatar != null) {
                AsyncImage(model = avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Icon(when (chat.type) { "event" -> Icons.Default.Event; "support" -> Icons.Default.Support; else -> Icons.Default.Person }, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary, fontSize = 15.sp)
                Text(time, fontSize = 12.sp, color = NexoryColors.TextSecondary)
            }
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(subtitle, fontSize = 13.sp, color = NexoryColors.TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                if (chat.unreadCount > 0) {
                    Box(modifier = Modifier.size(20.dp).background(NexoryColors.PrimaryBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${chat.unreadCount}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = NexoryColors.SurfaceMid, thickness = 0.5.dp)
    }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Заглушить") },
                onClick = { menuOpen = false; onMute() },
                leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = NexoryColors.TextSecondary) },
            )
            DropdownMenuItem(
                text = { Text(archiveLabel) },
                onClick = { menuOpen = false; onArchive() },
                leadingIcon = { Icon(Icons.Default.Archive, null, tint = NexoryColors.TextSecondary) },
            )
            DropdownMenuItem(
                text = { Text("Удалить чат") },
                onClick = { menuOpen = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = NexoryColors.Error) },
            )
        }
    }
}
