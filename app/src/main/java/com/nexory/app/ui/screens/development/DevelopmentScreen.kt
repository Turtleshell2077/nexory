package com.nexory.app.ui.screens.development

import android.content.Context
import android.content.Intent
import android.net.Uri
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

// ⚠️ ВЛАДЕЛЬЦУ (реквизиты приёма перевода):
// DONATION_URL — ссылка на платёжную страницу Т-банка, привязанную к твоему счёту.
// Плательщик открывает её, выбирает свой банк и переводит — ТВОЙ номер/карта нигде
// в приложении НЕ показываются, они «зашиты» в саму ссылку на стороне банка.
// Сделать её можно в Т-банке: «Собрать деньги» / «Мне переведут». Вид: https://www.tinkoff.ru/rm/xxxxx
// Сумма из поля подставляется в ссылку параметром ?amount= (если страница это поддерживает).
private const val DONATION_URL = "https://www.tinkoff.ru/rm/"

private data class Roadmap(val title: String, val text: String)

private val ROADMAP = listOf(
    Roadmap("Google Play", "Публикация приложения в официальном магазине Google Play."),
    Roadmap("Версия для iPhone", "Разработка приложения под iOS."),
    Roadmap("Яндекс.Карты", "Карта мероприятий рядом и построение маршрута до места."),
    Roadmap("Мощнее серверы", "Расширение инфраструктуры, чтобы всё работало быстро при росте числа пользователей."),
    Roadmap("Умный подбор", "Рекомендации мероприятий и людей по твоим интересам."),
    Roadmap("Рейтинги и отзывы", "Оценки организаторов и мероприятий — доверие и качество."),
    Roadmap("Оплата участия", "Оплата платных мероприятий прямо в приложении, безопасно."),
    Roadmap("Веб-версия", "Доступ к Nexory с компьютера через браузер."),
    Roadmap("Умные уведомления", "Гибкие и точные напоминания о том, что важно именно тебе."),
    Roadmap("Другие языки", "Поддержка нескольких языков интерфейса."),
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
    var roadmapExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }

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
            // 1. Вступление — простой текст, без смайликов
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(18.dp),
            ) {
                Text("Это раздел развития проекта", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nexory постоянно растёт и становится лучше. Мы регулярно добавляем новые " +
                        "возможности и исправляем недочёты. Здесь можно предложить своё улучшение " +
                        "и посмотреть, над чем мы работаем дальше.",
                    color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
            }

            // 2. Поле для предложения — над списком улучшений
            SectionTitle("Предложить улучшение")
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

            // 3. Раскрывающийся блок «Что хотим улучшить»
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoryColors.SurfaceDark),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { roadmapExpanded = !roadmapExpanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Что хотим улучшить", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                    Icon(
                        if (roadmapExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (roadmapExpanded) "Свернуть" else "Развернуть",
                        tint = NexoryColors.TextSecondary,
                    )
                }
                if (roadmapExpanded) {
                    ROADMAP.forEach { r ->
                        HorizontalDivider(color = NexoryColors.SurfaceMid, modifier = Modifier.padding(horizontal = 16.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Box(
                                modifier = Modifier.padding(top = 5.dp).size(7.dp).clip(RoundedCornerShape(4.dp))
                                    .background(NexoryColors.PrimaryBlue),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(r.title, color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(r.text, color = NexoryColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }

            // 4. Поддержать проект — оплата без раскрытия номера
            SectionTitle("Поддержать проект")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(16.dp),
            ) {
                Text(
                    "Поддержи проект любой суммой — это ускоряет выход новых функций. " +
                        "Оплата проходит через защищённую страницу банка: реквизиты получателя " +
                        "не раскрываются, привязывать ничего не нужно.",
                    color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() }.take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Сумма, ₽ (по желанию)", color = NexoryColors.TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Payments, null) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { openUrl(context, buildPayUrl(amount)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (amount.isBlank()) "Перейти к оплате" else "Поддержать на $amount ₽",
                                color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// Собираем ссылку на оплату: если указана сумма — добавляем ?amount=
private fun buildPayUrl(amount: String): String =
    if (amount.isBlank()) DONATION_URL else "$DONATION_URL?amount=$amount"

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary, letterSpacing = 0.5.sp)
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось открыть ссылку оплаты", Toast.LENGTH_SHORT).show()
    }
}
