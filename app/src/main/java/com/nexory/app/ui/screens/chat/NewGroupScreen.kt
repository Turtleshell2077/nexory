package com.nexory.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nexory.app.data.network.ApiError
import com.nexory.app.data.network.CreateGroupRequest
import com.nexory.app.data.network.FriendDto
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.UserAvatar
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.components.scrollOnFocus
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewGroupUiState(
    val friends:   List<FriendDto> = emptyList(),
    val selected:  Set<String>     = emptySet(),
    val isLoading: Boolean         = false,
    val isCreating: Boolean        = false,
    val createdChatId: String?     = null,
    val error:     String?         = null,
)

@HiltViewModel
class NewGroupViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {

    private val _state = MutableStateFlow(NewGroupUiState())
    val state = _state.asStateFlow()

    init { loadFriends() }

    private fun loadFriends() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val friends = api.getFriends()["friends"] ?: emptyList()
                _state.update { it.copy(friends = friends, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggle(userId: String) = _state.update {
        val set = it.selected.toMutableSet()
        if (!set.add(userId)) set.remove(userId)
        it.copy(selected = set, error = null)
    }

    fun create(title: String) {
        val members = _state.value.selected.toList()
        if (title.isBlank() || members.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            try {
                val response = api.createGroupChat(CreateGroupRequest(title.trim(), members))
                _state.update { it.copy(isCreating = false, createdChatId = response.chatId) }
            } catch (e: Exception) {
                _state.update { it.copy(isCreating = false, error = ApiError.message(e)) }
            }
        }
    }
}

/**
 * Создание групповой беседы: название + выбор участников из списка друзей.
 *
 * Участники выбираются только из друзей — это осознанное ограничение. Иначе
 * произвольного человека можно было бы затащить в беседу по нику, без всякого
 * его согласия, а это прямой путь к спаму.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    navController: NavController,
    viewModel: NewGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var title by remember { mutableStateOf("") }

    // Группа создана — открываем её, а сам экран убираем из стека,
    // чтобы «Назад» вёл в список чатов, а не обратно в форму
    LaunchedEffect(state.createdChatId) {
        state.createdChatId?.let { chatId ->
            navController.navigate(Screen.ChatDetail.route(chatId)) {
                popUpTo(Screen.Chats.route)
            }
        }
    }

    val canCreate = title.isNotBlank() && state.selected.isNotEmpty() && !state.isCreating

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Новая группа", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexoryColors.SurfaceDark)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
            ) {
                state.error?.let {
                    Text(it, color = NexoryColors.Error, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = { viewModel.create(title) },
                    enabled = canCreate,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexoryColors.PrimaryBlue,
                        disabledContainerColor = NexoryColors.SurfaceMid,
                    ),
                ) {
                    if (state.isCreating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (state.selected.isEmpty()) "Выберите участников"
                            else "Создать группу (${state.selected.size})",
                            color = if (canCreate) Color.White else NexoryColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp).scrollOnFocus(),
                placeholder = { Text("Название группы", color = NexoryColors.TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Groups, null, tint = NexoryColors.TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoryTextFieldColors(),
            )

            // Лента выбранных: при длинном списке друзей иначе не видно,
            // кого ты уже отметил
            if (state.selected.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.friends.filter { it.id in state.selected }.forEach { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp).clickable { viewModel.toggle(user.id) },
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                UserAvatar(
                                    url = user.avatarUrl,
                                    name = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                                    seed = user.id, size = 48.dp,
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(NexoryColors.SurfaceDark, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Cancel, "Убрать", tint = NexoryColors.TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                                color = NexoryColors.TextSecondary, fontSize = 11.sp, maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "Кого добавить",
                color = NexoryColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (state.friends.isEmpty() && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAddAlt, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Пока нет друзей", color = NexoryColors.TextPrimary, fontSize = 15.sp)
                        Text(
                            "Группа собирается из друзей — добавьте их на вкладке «Друзья»",
                            color = NexoryColors.TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.friends, key = { it.id }) { user ->
                        val checked = user.id in state.selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (checked) NexoryColors.PrimaryBlue.copy(alpha = 0.12f) else NexoryColors.SurfaceDark)
                                .clickable { viewModel.toggle(user.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null,
                                tint = if (checked) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            UserAvatar(
                                url = user.avatarUrl,
                                name = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                                seed = user.id, size = 44.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                                    color = NexoryColors.TextPrimary, fontWeight = FontWeight.Medium,
                                )
                                Text("@${user.username}", color = NexoryColors.TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
