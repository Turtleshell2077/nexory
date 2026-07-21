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
    val state by viewModel.uiState.collectAsState()
    var showSetPin by remember { mutableStateOf(false) }

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
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
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

            // ---- О приложении ----
            SettingsSectionLabel("О приложении")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Info, null, tint = NexoryColors.TextSecondary)
                Spacer(Modifier.width(12.dp))
                Text("Nexory", color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("v1.0.0", color = NexoryColors.TextSecondary, fontSize = 13.sp)
            }
        }
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
