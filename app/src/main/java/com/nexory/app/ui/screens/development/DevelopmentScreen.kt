package com.nexory.app.ui.screens.development

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.ExperimentalMaterial3Api

// ⚠️ ВЛАДЕЛЬЦУ: укажите свои реквизиты для перевода по СБП (номер телефона + банк).
// Перевод по номеру телефона через СБП анонимен и не требует привязки карт от отправителя.
private const val SBP_PHONE = "+7 985 144 88 24"
private const val SBP_BANK  = "Т-банк"

private data class Roadmap(val icon: String, val title: String, val text: String)

private val ROADMAP = listOf(
    Roadmap("📱", "Google Play", "Публикация приложения в официальном магазине Google Play."),
    Roadmap("🍎", "Версия для iPhone", "Разработка приложения под iOS."),
    Roadmap("🗺️", "Яндекс.Карты", "Карта мероприятий рядом и построение маршрута до места."),
    Roadmap("⚡", "Мощнее серверы", "Расширение инфраструктуры, чтобы всё летало при росте числа пользователей."),
    Roadmap("🤖", "Умный подбор", "Рекомендации мероприятий и людей по твоим интересам."),
    Roadmap("⭐", "Рейтинги и отзывы", "Оценки организаторов и мероприятий — доверие и качество."),
    Roadmap("💳", "Оплата участия", "Оплата платных мероприятий прямо в приложении, безопасно."),
    Roadmap("🌐", "Веб-версия", "Доступ к Nexory с компьютера через браузер."),
    Roadmap("🔔", "Умные уведомления", "Гибкие и точные напоминания о том, что важно именно тебе."),
    Roadmap("🌍", "Другие языки", "Поддержка нескольких языков интерфейса."),
)

@HiltViewModel
class DevelopmentViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {
    private val _sent = MutableStateFlow(false)
    val sent = _sent.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun sendSuggestion(text: String, onDone: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            try {
                api.createSupportTicket(mapOf("subject" to "Предложение по развитию", "body" to text))
                _sent.value = true
                onDone()
            } catch (_: Exception) {}
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentScreen(
    navController: NavController,
    viewModel: DevelopmentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    var suggestion by remember { mutableStateOf("") }
    var suggestionSent by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Развитие проекта", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Вступление
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(NexoryColors.DeepBlue, NexoryColors.Violet.copy(alpha = 0.7f))))
                    .padding(20.dp),
            ) {
                Column {
                    Text("Nexory растёт 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Мы делаем приложение для живого общения и классных мероприятий. " +
                            "Впереди много планов — и твоя поддержка помогает воплощать их быстрее.",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 19.sp,
                    )
                }
            }

            // Дорожная карта
            SectionTitle("Что хотим улучшить")
            ROADMAP.forEach { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexoryColors.SurfaceDark)
                        .padding(14.dp),
                ) {
                    Text(r.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(r.title, color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(r.text, color = NexoryColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }

            // Поддержать проект
            SectionTitle("Поддержать проект")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = NexoryColors.Error, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Донат по СБП", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Перевод по номеру телефона через СБП — быстро, анонимно и без привязки карт. " +
                        "Любая сумма помогает развитию проекта. Спасибо! 💙",
                    color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoryColors.SurfaceMid)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(SBP_PHONE, color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(SBP_BANK, color = NexoryColors.TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { copyToClipboard(context, SBP_PHONE) }) {
                        Icon(Icons.Default.ContentCopy, "Скопировать", tint = NexoryColors.PrimaryBlue)
                    }
                }
            }

            // Предложить идею
            SectionTitle("Предложить идею")
            if (suggestionSent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexoryColors.SurfaceDark)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = NexoryColors.PrimaryBlue)
                    Spacer(Modifier.width(10.dp))
                    Text("Спасибо! Мы прочитаем твоё предложение.", color = NexoryColors.TextSecondary, fontSize = 14.sp)
                }
            } else {
                OutlinedTextField(
                    value = suggestion,
                    onValueChange = { suggestion = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Что можно улучшить или добавить?", color = NexoryColors.TextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.sendSuggestion(suggestion) { suggestionSent = true; suggestion = "" } },
                    enabled = suggestion.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else { Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Отправить предложение") }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary, letterSpacing = 0.5.sp)
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("СБП", text))
    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
}
