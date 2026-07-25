package com.nexory.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nexory.app.data.local.ThemeMode
import com.nexory.app.ui.theme.NexoryColors
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var showSetPin by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    // Разрешение на уведомления (Android 13+) + диалог с объяснением перед запросом
    val notifPermission = com.nexory.app.ui.components.rememberNotificationPermission()
    var showNotifRationale by remember { mutableStateOf(false) }

    if (showNotifRationale) {
        AlertDialog(
            onDismissRequest = { showNotifRationale = false },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Notifications, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Разрешить уведомления?", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Уведомления нужны, чтобы вы вовремя узнавали о новых сообщениях в чатах, " +
                        "заявках в друзья и мероприятиях по вашим интересам.\n\n" +
                        "Разрешение используется только для этого. Отключить можно в любой момент здесь же.",
                    color = NexoryColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotifRationale = false; notifPermission.request() }) {
                    Text("Разрешить", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifRationale = false }) {
                    Text("Не сейчас", color = NexoryColors.TextSecondary)
                }
            },
        )
    }

    // Диалог удаления аккаунта: явное предупреждение о необратимости + подтверждение паролём
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleteState.isLoading) { showDeleteDialog = false; deletePassword = "" } },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.WarningAmber, null, tint = NexoryColors.Error, modifier = Modifier.size(36.dp)) },
            title = { Text("Удалить аккаунт?", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Будут безвозвратно удалены: профиль и фотография, созданные вами " +
                            "мероприятия, ваши сообщения, друзья и заявки, настройки. " +
                            "Восстановить аккаунт будет невозможно.",
                        color = NexoryColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it; viewModel.clearDeleteError() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !deleteState.isLoading,
                        label = { Text("Пароль для подтверждения") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = deleteState.error != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = com.nexory.app.ui.components.nexoryTextFieldColors(),
                    )
                    deleteState.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = NexoryColors.Error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAccount(deletePassword) },
                    enabled = !deleteState.isLoading && deletePassword.isNotBlank(),
                ) {
                    if (deleteState.isLoading) {
                        CircularProgressIndicator(color = NexoryColors.Error, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Удалить навсегда", color = NexoryColors.Error, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; deletePassword = "" },
                    enabled = !deleteState.isLoading,
                ) { Text("Отмена", color = NexoryColors.TextSecondary) }
            },
        )
    }

    // Экран установки PIN поверх настроек
    if (showSetPin) {
        com.nexory.app.ui.screens.security.PinLockScreen(
            mode = com.nexory.app.ui.screens.security.PinMode.SET,
            onPinSet = { viewModel.setPin(it) },
            onSuccess = { showSetPin = false },
            onCancel = { showSetPin = false },
        )
        return
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ---- Тема ----
            SettingsSectionLabel("Оформление")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark),
            ) {
                ThemeOption("Системная", Icons.Default.BrightnessAuto, state.themeMode == ThemeMode.SYSTEM) {
                    viewModel.setTheme(ThemeMode.SYSTEM)
                }
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                ThemeOption("Светлая", Icons.Default.LightMode, state.themeMode == ThemeMode.LIGHT) {
                    viewModel.setTheme(ThemeMode.LIGHT)
                }
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                ThemeOption("Тёмная", Icons.Default.DarkMode, state.themeMode == ThemeMode.DARK) {
                    viewModel.setTheme(ThemeMode.DARK)
                }
            }

            // ---- Уведомления ----
            SettingsSectionLabel("Уведомления")

            // Если системное разрешение не выдано, уведомления физически не придут
            // (Android 13+). Показываем понятное объяснение и кнопку запроса —
            // разрешение просим ровно в момент, когда оно нужно.
            if (notifPermission.shouldAsk) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexoryColors.PrimaryBlue.copy(alpha = 0.12f))
                        .clickable { showNotifRationale = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.NotificationsOff, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Разрешите уведомления", color = NexoryColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Без разрешения системы сообщения и события не придут",
                            color = NexoryColors.TextSecondary, fontSize = 12.sp)
                    }
                    Text("Разрешить", color = NexoryColors.PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark),
            ) {
                NotifyRow(
                    title = "Push-уведомления",
                    subtitle = "Главный переключатель",
                    checked = state.notificationsEnabled,
                    enabled = true,
                    onCheckedChange = { on ->
                        viewModel.setNotificationsEnabled(on)
                        // Включают уведомления — самое время попросить системное разрешение
                        if (on && notifPermission.shouldAsk) showNotifRationale = true
                    },
                )
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                NotifyRow(
                    title = "Сообщения в чатах",
                    subtitle = "Когда вам пишут",
                    checked = state.notifyMessages,
                    enabled = state.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotifyMessages(it) },
                )
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                NotifyRow(
                    title = "Мероприятия друзей",
                    subtitle = "Друг создал мероприятие",
                    checked = state.notifyFriendEvents,
                    enabled = state.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotifyFriendEvents(it) },
                )
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                NotifyRow(
                    title = "По моим интересам",
                    subtitle = "Новое мероприятие по твоим увлечениям",
                    checked = state.notifyInterestEvents,
                    enabled = state.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotifyInterestEvents(it) },
                )
            }

            // ---- Приватность ----
            SettingsSectionLabel("Кто видит мои контакты")
            Text(
                "Телефон и e-mail в профиле видят только те, кого вы выберете ниже.",
                color = NexoryColors.TextSecondary, fontSize = 12.sp,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark),
            ) {
                VisibilityOption("Все пользователи", Icons.Default.Public, state.profileVisibility == "all") { viewModel.setVisibility("all") }
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                VisibilityOption("Только друзья", Icons.Default.Group, state.profileVisibility == "friends") { viewModel.setVisibility("friends") }
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                VisibilityOption("Выбранные друзья", Icons.Default.PersonPin, state.profileVisibility == "selected") { viewModel.setVisibility("selected") }
            }
            if (state.profileVisibility == "selected") {
                OutlinedButton(
                    onClick = { navController.navigate(com.nexory.app.navigation.Screen.SelectFriends.route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
                ) {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выбрать друзей")
                }
            }

            // ---- Безопасность ----
            SettingsSectionLabel("Безопасность")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Lock, null, tint = NexoryColors.PrimaryBlue)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Вход по PIN-коду", color = NexoryColors.TextPrimary, fontSize = 15.sp)
                    Text(
                        if (state.pinEnabled) "Приложение запрашивает PIN при запуске" else "Защитить вход четырёхзначным кодом",
                        color = NexoryColors.TextSecondary, fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = state.pinEnabled,
                    onCheckedChange = { on -> if (on) showSetPin = true else viewModel.disablePin() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NexoryColors.PrimaryBlue,
                        uncheckedTrackColor = NexoryColors.SurfaceMid,
                    ),
                )
            }

            // ---- О приложении: версия + юр. документы ----
            SettingsSectionLabel("О приложении")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Info, null, tint = NexoryColors.TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    Text("Nexory", color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("v1.0.0", color = NexoryColors.TextSecondary, fontSize = 13.sp)
                }
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                // Документы должны быть доступны и из настроек (требование обоих сторов)
                LegalRow("Политика конфиденциальности") {
                    com.nexory.app.ui.components.openExternalUrl(context, com.nexory.app.NexoryConfig.PRIVACY_URL)
                }
                HorizontalDivider(color = NexoryColors.SurfaceMid)
                LegalRow("Пользовательское соглашение") {
                    com.nexory.app.ui.components.openExternalUrl(context, com.nexory.app.NexoryConfig.TERMS_URL)
                }
            }

            // ---- Аккаунт: удаление (требование Google Play) ----
            SettingsSectionLabel("Аккаунт")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark)
                    .clickable { showDeleteDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.DeleteForever, null, tint = NexoryColors.Error)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Удалить аккаунт", color = NexoryColors.Error, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Профиль и все данные будут удалены безвозвратно",
                        color = NexoryColors.TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = NexoryColors.TextSecondary)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LegalRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Shield, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = NexoryColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.OpenInNew, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary, letterSpacing = 0.5.sp)
}

@Composable
private fun NotifyRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha = if (enabled) 1f else 0.4f
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NexoryColors.TextPrimary.copy(alpha = alpha), fontSize = 15.sp)
            Text(subtitle, color = NexoryColors.TextSecondary.copy(alpha = alpha), fontSize = 12.sp)
        }
        Switch(
            checked = checked && enabled,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NexoryColors.PrimaryBlue,
                uncheckedTrackColor = NexoryColors.SurfaceMid,
            )
        )
    }
}

@Composable
private fun VisibilityOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = NexoryColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = NexoryColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(20.dp))
    }
}
