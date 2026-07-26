package com.nexory.app.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nexory.app.NexoryConfig
import com.nexory.app.data.local.SettingsManager
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.openExternalUrl
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val settings: SettingsManager,
) : ViewModel() {
    fun accept(onDone: () -> Unit) {
        viewModelScope.launch { settings.setLegalAccepted(); onDone() }
    }
}

/**
 * Экран согласия с юридическими документами.
 *
 * Показывается ДО экранов входа и регистрации — это прямое требование модерации RuStore
 * («необходимо добавить ссылку на ознакомление с политикой конфиденциальности перед
 * авторизацией и регистрацией пользователя»).
 *
 * Ключевые моменты для модерации:
 *  - ссылки на политику и соглашение кликабельны и открываются во внешнем браузере;
 *  - кнопка продолжения неактивна, пока пользователь явно не поставил галочку;
 *  - экран нельзя пропустить: он стоит перед Login в графе навигации.
 */
@Composable
fun ConsentScreen(
    navController: NavController,
    viewModel: ConsentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var agreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoryColors.DeepBlack)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PrivacyTip, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Добро пожаловать в Nexory",
            fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = NexoryColors.TextPrimary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Перед началом использования ознакомьтесь с тем, как мы обрабатываем ваши данные.",
            fontSize = 14.sp, color = NexoryColors.TextSecondary,
            textAlign = TextAlign.Center, lineHeight = 20.sp,
        )

        Spacer(Modifier.height(24.dp))

        // Ссылки на документы — отдельными нажимаемыми строками, чтобы модератор
        // и пользователь гарантированно их увидели и смогли открыть.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NexoryColors.SurfaceDark),
        ) {
            LegalLinkRow(
                icon = Icons.Default.Shield,
                title = "Политика конфиденциальности",
                subtitle = "Какие данные собираются и зачем",
                onClick = { openExternalUrl(context, NexoryConfig.PRIVACY_URL) },
            )
            HorizontalDivider(color = NexoryColors.SurfaceMid)
            LegalLinkRow(
                icon = Icons.Default.Description,
                title = "Пользовательское соглашение",
                subtitle = "Правила использования сервиса",
                onClick = { openExternalUrl(context, NexoryConfig.TERMS_URL) },
            )
        }

        Spacer(Modifier.height(20.dp))

        // Чекбокс согласия. Вся строка нажимаема — удобнее попадать пальцем.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { agreed = !agreed }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = NexoryColors.PrimaryBlue,
                    uncheckedColor = NexoryColors.TextSecondary,
                    checkmarkColor = Color.White,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Я прочитал(а) и принимаю Политику конфиденциальности и Пользовательское соглашение",
                fontSize = 13.sp, color = NexoryColors.TextPrimary,
                lineHeight = 19.sp, modifier = Modifier.padding(top = 14.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.accept {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            enabled = agreed,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    if (agreed) Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                    else Brush.linearGradient(listOf(NexoryColors.SurfaceMid, NexoryColors.SurfaceMid))
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Продолжить",
                    color = if (agreed) Color.White else NexoryColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Nexory не предназначен для лиц младше 14 лет",
            fontSize = 11.sp, color = NexoryColors.TextSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LegalLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NexoryColors.PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = NexoryColors.TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.OpenInNew, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}
