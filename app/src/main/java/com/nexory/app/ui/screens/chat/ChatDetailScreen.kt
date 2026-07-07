package com.nexory.app.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.MessageDto
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    chatId:        String,
    viewModel:     ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Загружаем чат при открытии экрана
    LaunchedEffect(chatId) { viewModel.loadChat(chatId) }

    // Автоскролл вниз при появлении нового сообщения
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    // Тап по аватару/названию открывает информацию о чате
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController.navigate(Screen.ChatInfo.route(chatId)) },
                    ) {
                        AsyncImage(
                            model              = uiState.chatAvatarUrl,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NexoryColors.SurfaceMid),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            uiState.chatTitle.ifBlank { "Чат" },
                            color      = NexoryColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NexoryColors.SurfaceDark
                ),
            )
        },
        bottomBar = {
            // ---- Поле ввода сообщения ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexoryColors.SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding(), // поднимаемся над клавиатурой
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Написать...", color = NexoryColors.TextSecondary) },
                    maxLines      = 4,
                    shape         = RoundedCornerShape(22.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = NexoryColors.PrimaryBlue,
                        unfocusedBorderColor    = NexoryColors.SurfaceMid,
                        focusedContainerColor   = NexoryColors.SurfaceMid,
                        unfocusedContainerColor = NexoryColors.SurfaceMid,
                        focusedTextColor        = NexoryColors.TextPrimary,
                        unfocusedTextColor      = NexoryColors.TextPrimary,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                // Кнопка отправки
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            viewModel.sendMessage(chatId, text)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd)
                            ),
                            CircleShape
                        ),
                ) {
                    Icon(Icons.Default.Send, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state          = listState,
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Кнопка "загрузить старые" при прокрутке вверх
            if (uiState.hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { viewModel.loadMoreMessages() }) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = NexoryColors.PrimaryBlue,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Загрузить предыдущие",
                                    color = NexoryColors.TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            items(uiState.messages, key = { it.id }) { msg ->
                val isMyMessage = msg.senderId == uiState.currentUserId
                MessageBubble(message = msg, isMyMessage = isMyMessage)
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageDto, isMyMessage: Boolean) {
    val alignment   = if (isMyMessage) Alignment.CenterEnd    else Alignment.CenterStart
    val bubbleColor = if (isMyMessage) NexoryColors.DeepBlue  else NexoryColors.SurfaceMid
    val textColor   = NexoryColors.TextPrimary

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(
            modifier = Modifier
                .align(alignment)
                .widthIn(max = 280.dp),
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start,
        ) {
            // Имя отправителя для чужих сообщений
            if (!isMyMessage) {
                Text(
                    text     = message.senderUsername,
                    fontSize = 11.sp,
                    color    = NexoryColors.LightViolet,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }

            // Пузырёк сообщения
            Box(
                modifier = Modifier
                    .background(
                        color  = bubbleColor,
                        shape  = RoundedCornerShape(
                            topStart    = if (isMyMessage) 16.dp else 4.dp,
                            topEnd      = if (isMyMessage) 4.dp  else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd   = 16.dp,
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text     = message.content,
                    fontSize = 15.sp,
                    color    = textColor,
                    lineHeight = 21.sp,
                )
            }

            // Время
            Text(
                text     = message.createdAt.drop(11).take(5), // "HH:mm"
                fontSize = 10.sp,
                color    = NexoryColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}