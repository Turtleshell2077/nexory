package com.nexory.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.NexoryBottomBar
import com.nexory.app.ui.theme.NexoryColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAvatarFull by remember { mutableStateOf(false) }   // круг (тап)
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showAvatarRect by remember { mutableStateOf(false) }   // прямоугольник (потянуть вниз)

    // Настоящая фотография (а не выбранный вариант оформления). Полноэкранный
    // просмотр имеет смысл только для неё.
    val hasRealPhoto = uiState.user?.avatarUrl.let {
        com.nexory.app.ui.components.AvatarPresets.isRealPhoto(it)
    }

    val cropAvatar = com.nexory.app.ui.components.rememberImageCropper(circle = true) { cropped ->
        viewModel.uploadAvatar(cropped)
    }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { cropAvatar(it) } }

    // Все действия с аватаром — в одном листе: галерея, готовые варианты, удаление.
    // Объявлен после avatarPicker, т.к. ссылается на него.
    if (showAvatarPicker) {
        com.nexory.app.ui.components.AvatarPickerSheet(
            currentUrl = uiState.user?.avatarUrl,
            userName = uiState.user?.displayName?.takeIf { it.isNotBlank() } ?: uiState.user?.username,
            onPickPhoto = { avatarPicker.launch("image/*") },
            onRemove = { viewModel.removeAvatar() },
            onDismiss = { showAvatarPicker = false },
        )
    }

    // Обновляем профиль при возвращении на экран (после редактирования)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadProfile()
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        bottomBar      = { NexoryBottomBar(navController, Screen.Profile.route) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Шапка с градиентом — потянуть вниз, чтобы открыть фото прямоугольником
            var headerDrag by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(NexoryColors.DeepBlue, NexoryColors.DeepBlack)
                        )
                    )
                    .pointerInput(uiState.user?.avatarUrl) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (headerDrag > 60f && hasRealPhoto) showAvatarRect = true
                                headerDrag = 0f
                            },
                        ) { _, dragAmount -> headerDrag += dragAmount }
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(bottom = 20.dp),
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // Нет фото — показываем тот же генеративный аватар, который видят
                        // другие пользователи, чтобы владелец профиля понимал, как выглядит
                        com.nexory.app.ui.components.UserAvatar(
                            url  = uiState.user?.avatarUrl,
                            name = uiState.user?.displayName?.takeIf { it.isNotBlank() } ?: uiState.user?.username,
                            seed = uiState.user?.id,
                            size = 88.dp,
                            // Открываем полноэкранно только настоящее фото: у выбранного
                            // варианта оформления открывать нечего — раньше это давало
                            // чёрный экран, т.к. Coil не мог загрузить "preset:N" как URL
                            modifier = Modifier.clickable(enabled = hasRealPhoto) { showAvatarFull = true },
                        )
                        if (uiState.uploadingAvatar) {
                            Box(
                                modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.Black.copy(0.4f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        }
                        // Синий кружок — открыть выбор аватара
                        // (галерея / готовые варианты / удалить фото)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(NexoryColors.PrimaryBlue, CircleShape)
                                .clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                    // Кнопки «Удалить фото» здесь больше нет — она загромождала экран.
                    // Все действия с аватаром собраны в AvatarPickerSheet, который
                    // открывается нажатием на синий кружок у аватара.

                    // Ошибку загрузки/удаления всё же показываем: раньше сбой был молчаливым
                    uiState.error?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            err,
                            color = NexoryColors.Error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text       = uiState.user?.displayName?.takeIf { it.isNotBlank() }
                                        ?: uiState.user?.username ?: "...",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = NexoryColors.TextPrimary,
                    )
                    // Ник переехал в блок «Информация» — под аватаром остаются
                    // только имя и статус, так шапка выглядит спокойнее.

                    // Под аватаром показываем короткий СТАТУС.
                    // «О себе» (bio) переехало вниз блока информации, после email.
                    uiState.user?.status?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text     = it,
                            fontSize = 13.sp,
                            color    = NexoryColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Баннер: подтвердите почту
            if (uiState.user != null && uiState.user?.emailVerified == false) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexoryColors.Violet.copy(alpha = 0.15f))
                        .clickable { navController.navigate(Screen.VerifyEmail.route) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.MarkEmailUnread, null, tint = NexoryColors.Violet, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Подтвердите почту", color = NexoryColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Нажмите, чтобы ввести код из письма", color = NexoryColors.TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = NexoryColors.TextSecondary)
                }
            }

            // Информация о пользователе — текстом, с заголовком раздела
            val user = uiState.user
            if (user != null) {
                // Ник есть всегда, поэтому блок информации показываем всегда
                val hasBasics = true
                val interests  = user.sports?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

                if (hasBasics) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(NexoryColors.SurfaceDark, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Информация", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = NexoryColors.TextSecondary, letterSpacing = 0.5.sp)
                        // Ник — первой строкой информации
                        user.username.takeIf { it.isNotBlank() }?.let { InfoTextRow("Никнейм", "@$it") }
                        user.city?.takeIf { it.isNotBlank() }?.let { InfoTextRow("Город", it) }
                        user.age?.let { InfoTextRow("Возраст", "$it") }
                        user.phone?.takeIf { it.isNotBlank() }?.let { InfoTextRow("Телефон", it) }
                        user.email?.takeIf { it.isNotBlank() }?.let { InfoTextRow("Email", it) }
                        // «О себе» — в самом низу блока, после email (перенесено из-под аватара)
                        user.bio?.takeIf { it.isNotBlank() }?.let {
                            HorizontalDivider(color = NexoryColors.SurfaceMid, thickness = 0.5.dp)
                            Text("О себе", fontSize = 12.sp, color = NexoryColors.TextSecondary)
                            Text(it, fontSize = 14.sp, color = NexoryColors.TextPrimary, lineHeight = 20.sp)
                        }
                    }
                }

                // Любимые увлечения — в рамке
                if (interests.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(NexoryColors.SurfaceDark, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                    ) {
                        Text("Увлечения", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = NexoryColors.TextSecondary, letterSpacing = 0.5.sp)
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

            Spacer(Modifier.height(8.dp))

            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileMenuItem(
                    icon    = Icons.Default.Edit,
                    label   = "Редактировать профиль",
                    color   = NexoryColors.PrimaryBlue,
                    onClick = { navController.navigate(Screen.EditProfile.route) },
                )
                ProfileMenuItem(
                    icon    = Icons.Default.Settings,
                    label   = "Настройки",
                    color   = NexoryColors.Violet,
                    onClick = { navController.navigate(Screen.Settings.route) },
                )
                ProfileMenuItem(
                    icon    = Icons.Default.Favorite,
                    label   = "Развитие проекта",
                    color   = NexoryColors.Error,
                    onClick = { navController.navigate(Screen.Development.route) },
                )
                ProfileMenuItem(
                    icon    = Icons.Default.SupportAgent,
                    label   = "Поддержка",
                    color   = NexoryColors.LightViolet,
                    onClick = { navController.navigate(Screen.Support.route) },
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                Spacer(Modifier.height(8.dp))
                ProfileMenuItem(
                    icon    = Icons.Default.Logout,
                    label   = "Выйти из аккаунта",
                    color   = NexoryColors.Error,
                    onClick = { showLogoutDialog = true },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Аватар крупно, круглый (тап) — на тёмном фоне, чтобы видны границы
    if (showAvatarFull && hasRealPhoto) {
        Dialog(onDismissRequest = { showAvatarFull = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { showAvatarFull = false },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = uiState.user?.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(300.dp).clip(CircleShape).background(NexoryColors.SurfaceMid),
                )
            }
        }
    }

    // Аватар во весь размер, прямоугольный (потянуть вниз)
    if (showAvatarRect && hasRealPhoto) {
        Dialog(onDismissRequest = { showAvatarRect = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { showAvatarRect = false },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = uiState.user?.avatarUrl, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = NexoryColors.SurfaceDark,
            shape            = RoundedCornerShape(20.dp),
            title  = {
                Text("Выйти из аккаунта?", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold)
            },
            text   = {
                Text(
                    "Вы уверены, что хотите выйти? Токен будет удалён с устройства.",
                    color    = NexoryColors.TextSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout(); showLogoutDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = NexoryColors.Error),
                    shape   = RoundedCornerShape(10.dp),
                ) { Text("Выйти", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена", color = NexoryColors.PrimaryBlue)
                }
            }
        )
    }
}

@Composable
private fun InfoTextRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NexoryColors.TextSecondary, fontSize = 14.sp)
        Text(value, color = NexoryColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileMenuItem(
    icon:    ImageVector,
    label:   String,
    color:   Color,
    onClick: () -> Unit,
    badge:   String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NexoryColors.SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text     = label,
            fontSize = 15.sp,
            color    = NexoryColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .background(NexoryColors.PrimaryBlue, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint     = NexoryColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
