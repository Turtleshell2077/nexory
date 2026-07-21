package com.nexory.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.data.network.UserDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- ViewModel ----

data class UserProfileUiState(
    val user:         UserDto? = null,
    val isLoading:    Boolean  = false,
    val friendStatus: String   = "none",  // self|friends|pending_out|pending_in|none
    val actionLoading: Boolean = false,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileUiState())
    val state = _state.asStateFlow()
    private var currentId: String = ""

    fun load(userId: String) {
        currentId = userId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = api.getUserProfile(userId)
                val m = response["user"] as? Map<*, *>
                val user = m?.let {
                    UserDto(
                        id          = it["id"] as? String ?: "",
                        username    = it["username"] as? String ?: "",
                        avatarUrl   = it["avatar_url"] as? String,
                        bio         = it["bio"] as? String,
                        displayName = it["display_name"] as? String,
                        city        = it["city"] as? String,
                        country     = it["country"] as? String,
                        sports      = it["sports"] as? String,
                        age         = (it["age"] as? Double)?.toInt(),
                        phone       = it["phone"] as? String,
                        email       = it["email"] as? String,
                    )
                }
                val status = (m?.get("friend_status") as? String) ?: "none"
                _state.update { it.copy(user = user, friendStatus = status, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(actionLoading = true) }
            try { block() } catch (_: Exception) {}
            // Перечитываем статус после действия
            load(currentId)
            _state.update { it.copy(actionLoading = false) }
        }
    }

    fun sendFriendRequest(userId: String) = act { api.sendFriendRequest(mapOf("addresseeId" to userId)) }
    fun cancelFriendRequest(userId: String) = act { api.cancelFriendRequest(userId) }
    fun acceptFriendRequest(userId: String) = act { api.acceptFriendRequest(mapOf("requesterId" to userId)) }
    fun removeFriend(userId: String) = act { api.removeFriend(userId) }

    fun openDirectChat(userId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.getOrCreateDirectChat(mapOf("peerId" to userId))
                onChatReady(response.chatId)
            } catch (_: Exception) {}
        }
    }
}

// ---- UI ----

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    userId:        String,
    viewModel:     UserProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAvatarFull by remember { mutableStateOf(false) }
    var showAvatarRect by remember { mutableStateOf(false) }
    LaunchedEffect(userId) { viewModel.load(userId) }

    if (showAvatarFull && state.user?.avatarUrl != null) {
        Dialog(onDismissRequest = { showAvatarFull = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { showAvatarFull = false }, contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = state.user?.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(300.dp).clip(CircleShape).background(NexoryColors.SurfaceMid),
                )
            }
        }
    }
    if (showAvatarRect && state.user?.avatarUrl != null) {
        Dialog(onDismissRequest = { showAvatarRect = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { showAvatarRect = false }, contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = state.user?.avatarUrl, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(state.user?.displayName?.takeIf { it.isNotBlank() } ?: state.user?.username ?: "Профиль",
                        color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        }
    ) { padding ->
        if (state.isLoading && state.user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NexoryColors.PrimaryBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            var avDrag by remember { mutableStateOf(0f) }
            val avatarUrl = state.user?.avatarUrl
            com.nexory.app.ui.components.UserAvatar(
                url = avatarUrl,
                name = state.user?.displayName?.takeIf { it.isNotBlank() } ?: state.user?.username,
                seed = userId,
                size = 96.dp,
                modifier = Modifier
                    .clickable(enabled = avatarUrl != null) { showAvatarFull = true }
                    .pointerInput(avatarUrl) {
                        detectVerticalDragGestures(
                            onDragEnd = { if (avDrag > 50f && avatarUrl != null) showAvatarRect = true; avDrag = 0f },
                        ) { _, d -> avDrag += d }
                    },
            )
            Spacer(Modifier.height(12.dp))
            Text(state.user?.displayName?.takeIf { it.isNotBlank() } ?: state.user?.username ?: "...",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)
            if (state.user?.displayName?.isNotBlank() == true) {
                Text("@${state.user?.username}", fontSize = 13.sp, color = NexoryColors.TextSecondary)
            }
            state.user?.bio?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = NexoryColors.TextSecondary,
                    modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Кнопки действий (для чужого профиля). Свой профиль — только пометка.
            if (state.friendStatus == "self") {
                Text("Это ваш профиль", color = NexoryColors.TextSecondary, fontSize = 14.sp)
            } else
            Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.openDirectChat(userId) { chatId -> navController.navigate(Screen.ChatDetail.route(chatId)) } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                ) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Написать")
                }

                // Кнопка дружбы зависит от статуса
                when (state.friendStatus) {
                    "friends" -> OutlinedButton(
                        onClick  = { viewModel.removeFriend(userId) },
                        enabled  = !state.actionLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, NexoryColors.PrimaryBlue),
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Друзья")
                    }
                    "pending_out" -> OutlinedButton(
                        onClick  = { viewModel.cancelFriendRequest(userId) },
                        enabled  = !state.actionLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.TextSecondary),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, NexoryColors.SurfaceMid),
                    ) {
                        Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Отменить")
                    }
                    "pending_in" -> Button(
                        onClick  = { viewModel.acceptFriendRequest(userId) },
                        enabled  = !state.actionLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NexoryColors.Violet),
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Принять")
                    }
                    else -> OutlinedButton(
                        onClick  = { viewModel.sendFriendRequest(userId) },
                        enabled  = !state.actionLoading,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, NexoryColors.PrimaryBlue),
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Добавить")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Информация
            val u = state.user
            if (u != null) {
                val hasInfo = !u.city.isNullOrBlank() || u.age != null || !u.country.isNullOrBlank() ||
                    !u.phone.isNullOrBlank() || !u.email.isNullOrBlank()
                if (hasInfo) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            .background(NexoryColors.SurfaceDark, RoundedCornerShape(14.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Информация", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary)
                        u.city?.takeIf { it.isNotBlank() }?.let { InfoLineLR("Город", it) }
                        u.country?.takeIf { it.isNotBlank() }?.let { InfoLineLR("Страна", it) }
                        u.age?.let { InfoLineLR("Возраст", "$it") }
                        u.phone?.takeIf { it.isNotBlank() }?.let { InfoLineLR("Телефон", it) }
                        u.email?.takeIf { it.isNotBlank() }?.let { InfoLineLR("Email", it) }
                    }
                }
                val interests = u.sports?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                if (interests.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            .background(NexoryColors.SurfaceDark, RoundedCornerShape(14.dp)).padding(16.dp),
                    ) {
                        Text("Увлечения", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            interests.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, NexoryColors.PrimaryBlue.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                ) {
                                    Text(item, color = NexoryColors.PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoLineLR(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NexoryColors.TextSecondary, fontSize = 14.sp)
        Text(value, color = NexoryColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
