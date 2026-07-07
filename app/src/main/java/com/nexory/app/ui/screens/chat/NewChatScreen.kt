package com.nexory.app.ui.screens.chat

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.FriendDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.screens.friends.FriendsViewModel
import com.nexory.app.ui.theme.NexoryColors
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    navController: NavController,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    // Что показываем: при пустом поиске — друзей, иначе — результаты поиска
    val list: List<FriendDto> = if (state.searchQuery.isBlank()) state.friends else state.searchResults

    fun startChat(userId: String) {
        viewModel.openDirectChat(userId) { chatId ->
            navController.navigate(Screen.ChatDetail.route(chatId)) {
                popUpTo(Screen.Chats.route)
            }
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Новый чат", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::search,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Поиск по нику или телефону", color = NexoryColors.TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NexoryColors.TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoryTextFieldColors(),
            )

            if (state.searchQuery.isBlank()) {
                Text("Ваши друзья", color = NexoryColors.TextSecondary, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.searchQuery.isBlank()) "Пока нет друзей для чата" else "Никого не найдено",
                        color = NexoryColors.TextSecondary,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { user ->
                        val isSelf = user.id == state.myUserId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NexoryColors.SurfaceDark)
                                .clickable(enabled = !isSelf) { startChat(user.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(NexoryColors.SurfaceMid),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                (user.displayName?.takeIf { it.isNotBlank() } ?: user.username) + if (isSelf) " (вы)" else "",
                                color = if (isSelf) NexoryColors.TextSecondary else NexoryColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelf) {
                                Text("Это вы", color = NexoryColors.TextSecondary, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Chat, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
