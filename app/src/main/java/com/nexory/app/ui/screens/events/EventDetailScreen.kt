package com.nexory.app.ui.screens.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors

@Composable
fun EventDetailScreen(
    navController: NavController,
    eventId: String,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPriceInfo by remember { mutableStateOf(false) }
    var ownerMenu by remember { mutableStateOf(false) }
    var showParticipants by remember { mutableStateOf(false) }
    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    // Переход в личный чат с организатором, когда он создан
    LaunchedEffect(uiState.openDirectChatId) {
        uiState.openDirectChatId?.let { chatId ->
            viewModel.consumeOpenChat()
            navController.navigate(Screen.ChatDetail.route(chatId))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = NexoryColors.SurfaceDark,
            shape            = RoundedCornerShape(20.dp),
            title  = { Text("Удалить мероприятие?", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text   = { Text("Мероприятие и его чат будут удалены безвозвратно.", color = NexoryColors.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; viewModel.deleteEvent(eventId) { navController.popBackStack() } },
                    colors  = ButtonDefaults.buttonColors(containerColor = NexoryColors.Error),
                    shape   = RoundedCornerShape(10.dp),
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = NexoryColors.PrimaryBlue) }
            },
        )
    }

    if (showPriceInfo) {
        AlertDialog(
            onDismissRequest = { showPriceInfo = false },
            containerColor   = NexoryColors.SurfaceDark,
            shape            = RoundedCornerShape(16.dp),
            title  = { Text("За что оплата", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text   = { Text(uiState.event?.priceDescription ?: "", color = NexoryColors.TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showPriceInfo = false }) { Text("Понятно", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold) }
            },
        )
    }

    // Управление участниками (создатель)
    if (showParticipants && uiState.event != null) {
        val creatorId = uiState.event!!.creatorId
        AlertDialog(
            onDismissRequest = { showParticipants = false },
            containerColor   = NexoryColors.SurfaceDark,
            shape            = RoundedCornerShape(20.dp),
            title  = { Text("Участники — ${uiState.participants.size}", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text   = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.participants.forEach { p ->
                        val isCreator = p.id == creatorId
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = p.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(NexoryColors.SurfaceMid)
                                    .clickable { showParticipants = false; navController.navigate(Screen.UserProfile.route(p.id)) },
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.username, color = NexoryColors.TextPrimary, fontSize = 14.sp)
                                Text(
                                    when { isCreator -> "Организатор"; p.role == "moderator" -> "Модератор"; else -> "Участник" },
                                    color = NexoryColors.TextSecondary, fontSize = 11.sp,
                                )
                            }
                            if (!isCreator) {
                                // Повысить/понизить
                                IconButton(onClick = {
                                    viewModel.setParticipantRole(eventId, p.id, if (p.role == "moderator") "participant" else "moderator")
                                }) {
                                    Icon(
                                        if (p.role == "moderator") Icons.Default.StarBorder else Icons.Default.Star,
                                        contentDescription = "Роль", tint = NexoryColors.Violet, modifier = Modifier.size(20.dp),
                                    )
                                }
                                // Исключить
                                IconButton(onClick = { viewModel.kickParticipant(eventId, p.id) }) {
                                    Icon(Icons.Default.PersonRemove, "Исключить", tint = NexoryColors.Error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParticipants = false }) { Text("Закрыть", color = NexoryColors.PrimaryBlue) }
            },
        )
    }

    Scaffold(containerColor = NexoryColors.DeepBlack) { padding ->
        when {
            uiState.isLoading && uiState.event == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NexoryColors.PrimaryBlue)
                }
            }
            uiState.event == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(uiState.error ?: "Мероприятие не найдено", color = NexoryColors.TextSecondary)
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Назад", color = NexoryColors.PrimaryBlue)
                        }
                    }
                }
            }
            else -> {
                val event = uiState.event!!
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

                    // Обложка + кнопка назад
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                            if (event.coverUrl != null) {
                                AsyncImage(
                                    model = event.coverUrl, contentDescription = null,
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                                )
                                Box(modifier = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color.Transparent, NexoryColors.DeepBlack))
                                ))
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(
                                    Brush.linearGradient(listOf(NexoryColors.DeepBlue, NexoryColors.Violet))
                                ), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Event, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(80.dp))
                                }
                            }
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.padding(12.dp).background(Color.Black.copy(0.4f), CircleShape),
                            ) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                            }
                            // 3 точки для создателя
                            if (uiState.isMyEvent) {
                                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                    IconButton(
                                        onClick = { ownerMenu = true },
                                        modifier = Modifier.padding(12.dp).background(Color.Black.copy(0.4f), CircleShape),
                                    ) {
                                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                                    }
                                    DropdownMenu(expanded = ownerMenu, onDismissRequest = { ownerMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Редактировать") },
                                            onClick = { ownerMenu = false; navController.navigate(Screen.EditEvent.route(eventId)) },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Участники") },
                                            onClick = { ownerMenu = false; showParticipants = true },
                                            leadingIcon = { Icon(Icons.Default.People, null) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Удалить") },
                                            onClick = { ownerMenu = false; showDeleteConfirm = true },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = NexoryColors.Error) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Заголовок и мета
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            event.category?.let {
                                Text(it.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NexoryColors.LightViolet)
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(event.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)

                            Spacer(Modifier.height(16.dp))

                            // Информация — чистые текстовые строки без иконок
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .background(NexoryColors.SurfaceDark, RoundedCornerShape(14.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                DetailLine("Когда", formatEventDateTime(event.startsAt, event.endsAt))
                                DetailLine("Место", event.address)
                                event.metro?.takeIf { it.isNotBlank() }?.let { DetailLine("Метро", "м. $it") }
                                event.eventType?.takeIf { it.isNotBlank() }?.let { DetailLine("Тип", it) }
                                event.skillLevel?.takeIf { it.isNotBlank() }?.let { DetailLine("Уровень", it) }
                                DetailLine(
                                    "Участники",
                                    if (event.maxParticipants != null) "${event.participantCount} из ${event.maxParticipants}"
                                    else "${event.participantCount}",
                                )
                                // Цена + кружок информации (за что платить)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Стоимость", color = NexoryColors.TextSecondary, fontSize = 14.sp)
                                        if (!event.priceDescription.isNullOrBlank()) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier.size(18.dp)
                                                    .background(NexoryColors.SurfaceMid, CircleShape)
                                                    .clickable { showPriceInfo = true },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Default.QuestionMark, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }
                                    Text(formatPrice(event.price), color = NexoryColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                // Дата создания — ненавязчиво, отдельной строкой снизу
                                formatCreatedAt(event.createdAt).takeIf { it.isNotBlank() }?.let {
                                    HorizontalDivider(color = NexoryColors.SurfaceMid, thickness = 0.5.dp)
                                    Text(it, color = NexoryColors.TextSecondary, fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Организатор
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NexoryColors.SurfaceDark)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                com.nexory.app.ui.components.UserAvatar(
                                    url = event.creatorAvatar, name = event.creatorUsername,
                                    seed = event.creatorId, size = 40.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Организатор", fontSize = 11.sp, color = NexoryColors.TextSecondary)
                                    Text(event.creatorUsername, fontSize = 15.sp, color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                if (!uiState.isMyEvent) {
                                    IconButton(
                                        onClick = { viewModel.messageCreator() },
                                        modifier = Modifier.background(NexoryColors.PrimaryBlue.copy(0.15f), CircleShape),
                                    ) {
                                        Icon(Icons.Default.Send, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Описание
                    item {
                        if (!event.description.isNullOrBlank()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Text("Описание", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                Text(event.description, fontSize = 14.sp, color = NexoryColors.TextSecondary, lineHeight = 22.sp)
                            }
                        }
                    }

                    // Участники
                    item {
                        if (uiState.participants.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text("Участники", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(uiState.participants) { participant ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { navController.navigate(Screen.UserProfile.route(participant.id)) }
                                                .padding(4.dp),
                                        ) {
                                            com.nexory.app.ui.components.UserAvatar(
                                                url = participant.avatarUrl, name = participant.username,
                                                seed = participant.id, size = 48.dp,
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(participant.username, fontSize = 11.sp, color = NexoryColors.TextSecondary, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Действия
                    item {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            uiState.error?.let {
                                Text(it, color = NexoryColors.Error, fontSize = 13.sp)
                            }

                            val isFull = event.maxParticipants != null && event.participantCount >= event.maxParticipants

                            if (!uiState.isMyEvent) {
                                // Кнопка записи / отписки
                                GradientButton(
                                    text    = when {
                                        uiState.isJoined     -> "Отписаться"
                                        isFull               -> "Мест нет"
                                        else                 -> "Записаться"
                                    },
                                    loading = uiState.actionLoading,
                                    enabled = uiState.isJoined || !isFull,
                                    danger  = uiState.isJoined,
                                ) {
                                    if (uiState.isJoined) viewModel.leaveEvent(eventId) else viewModel.joinEvent(eventId)
                                }
                            }

                            // Чат мероприятия — доступен участникам и создателю
                            if ((uiState.isJoined || uiState.isMyEvent) && uiState.eventChatId != null) {
                                OutlinedActionButton(Icons.Default.Forum, "Чат мероприятия") {
                                    navController.navigate(Screen.ChatDetail.route(uiState.eventChatId!!))
                                }
                            }

                            // Редактирование/удаление/участники — в меню «три точки» сверху справа
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = NexoryColors.TextSecondary)
        Text(value, fontSize = 14.sp, color = NexoryColors.TextPrimary, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun GradientButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    danger: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled  = enabled && !loading,
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                when {
                    !enabled -> Brush.linearGradient(listOf(NexoryColors.SurfaceMid, NexoryColors.SurfaceMid))
                    danger   -> Brush.linearGradient(listOf(NexoryColors.Error, NexoryColors.Error.copy(0.7f)))
                    else     -> Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                }
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
private fun OutlinedActionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
        border   = BorderStroke(1.dp, NexoryColors.PrimaryBlue),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
