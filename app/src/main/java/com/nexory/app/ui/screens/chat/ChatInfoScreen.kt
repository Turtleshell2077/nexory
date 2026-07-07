package com.nexory.app.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.ChatInfo
import com.nexory.app.data.network.EventDto
import com.nexory.app.data.network.MediaUploader
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.data.network.UserDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.screens.events.formatEventDateTime
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- ViewModel ----

data class ChatInfoUiState(
    val chat:      ChatInfo?      = null,
    val members:   List<UserDto>  = emptyList(),
    val event:     EventDto?      = null,
    val isLoading: Boolean        = false,
    val uploading: Boolean        = false,
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val api:      NexoryApi,
    private val uploader: MediaUploader,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatInfoUiState())
    val state = _state.asStateFlow()

    private var chatId: String = ""

    fun load(id: String) {
        chatId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val r = api.getChatInfo(id)
                _state.update { it.copy(chat = r.chat, members = r.members, event = r.event, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun changeAvatar(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(uploading = true) }
            val url = uploader.upload(uri)
            if (url != null) {
                try {
                    api.updateChatAvatar(chatId, mapOf("avatar_url" to url))
                    _state.update { it.copy(chat = it.chat?.copy(avatarUrl = url)) }
                } catch (_: Exception) {}
            }
            _state.update { it.copy(uploading = false) }
        }
    }
}

// ---- UI ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    navController: NavController,
    chatId: String,
    viewModel: ChatInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(chatId) { viewModel.load(chatId) }

    val cropAvatar = com.nexory.app.ui.components.rememberImageCropper(circle = true) { cropped ->
        viewModel.changeAvatar(cropped)
    }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { cropAvatar(it) } }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Информация о чате", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))

            // Аватар чата
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier.size(110.dp).clip(CircleShape).background(NexoryColors.SurfaceMid),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.chat?.avatarUrl != null) {
                        AsyncImage(model = state.chat?.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(
                            if (state.chat?.type == "event") Icons.Default.Event else Icons.Default.Person,
                            null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(48.dp),
                        )
                    }
                    if (state.uploading) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                if (state.chat?.canEditAvatar == true) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd)), CircleShape)
                            .clickable { picker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(state.chat?.title ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)
            Spacer(Modifier.height(20.dp))

            // Карточка мероприятия (для event-чата)
            state.event?.let { event ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionHeader("Мероприятие")
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoryColors.SurfaceDark)
                            .clickable { navController.navigate(Screen.EventDetail.route(event.id)) }
                            .padding(16.dp),
                    ) {
                        Text(event.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        InfoLine(Icons.Default.CalendarToday, formatEventDateTime(event.startsAt, event.endsAt))
                        Spacer(Modifier.height(4.dp))
                        InfoLine(Icons.Default.LocationOn, event.address)
                        if (!event.description.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(event.description, fontSize = 13.sp, color = NexoryColors.TextSecondary, lineHeight = 19.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // Участники
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionHeader("Участники — ${state.members.size}")
                Spacer(Modifier.height(8.dp))
                state.members.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { navController.navigate(Screen.UserProfile.route(member.id)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = member.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(NexoryColors.SurfaceMid),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(member.username, color = NexoryColors.TextPrimary, fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary, letterSpacing = 0.5.sp)
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, color = NexoryColors.TextSecondary)
    }
}
