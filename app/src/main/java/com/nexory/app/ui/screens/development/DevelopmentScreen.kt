package com.nexory.app.ui.screens.development

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
import com.nexory.app.ui.components.scrollOnFocus
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.ExperimentalMaterial3Api

// Реквизиты приёма переводов живут в data/donation/BankLinkDonationService.kt —
// экран о них ничего не знает и работает через интерфейс DonationService.

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
    private val donationService: com.nexory.app.data.donation.DonationService,
) : ViewModel() {
    private val _sent = MutableStateFlow(false)
    val sent = _sent.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    /** Ошибка отправки предложения — раньше молча проглатывалась. */
    private val _suggestionError = MutableStateFlow<String?>(null)
    val suggestionError = _suggestionError.asStateFlow()

    /** Что умеет текущая реализация оплаты — UI рисует флоу по этим флагам. */
    val donationCapabilities = donationService.capabilities

    private val _donationState = MutableStateFlow<DonationUiState>(DonationUiState.Idle)
    val donationState = _donationState.asStateFlow()

    fun sendSuggestion(text: String, onDone: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _suggestionError.value = null
            try {
                api.createSupportTicket(mapOf("subject" to "Предложение по развитию", "body" to text))
                _sent.value = true
                onDone()
            } catch (e: Exception) {
                // Раньше исключение игнорировалось и кнопка просто «ничего не делала»
                _suggestionError.value = com.nexory.app.data.network.ApiError.message(e)
            }
            _loading.value = false
        }
    }

    fun clearSuggestionError() { _suggestionError.value = null }

    /** Начать пожертвование. Все ветки результата приводим к состоянию для UI. */
    fun donate(context: android.content.Context) {
        viewModelScope.launch {
            _donationState.value = DonationUiState.Starting
            val result = donationService.start(context)
            _donationState.value = when (result) {
                is com.nexory.app.data.donation.DonationResult.OpenedExternally ->
                    DonationUiState.AwaitingReturn
                is com.nexory.app.data.donation.DonationResult.Failed -> when (result.reason) {
                    com.nexory.app.data.donation.DonationError.NO_NETWORK ->
                        DonationUiState.Error("Нет подключения к интернету. Страница оплаты не откроется — проверьте связь и попробуйте снова")
                    com.nexory.app.data.donation.DonationError.CANNOT_OPEN ->
                        DonationUiState.Error("Не удалось открыть страницу оплаты. Проверьте, что на устройстве установлен браузер")
                    com.nexory.app.data.donation.DonationError.NOT_CONFIGURED ->
                        DonationUiState.Error("Приём переводов пока не настроен. Мы включим его в одном из следующих обновлений")
                }
            }
        }
    }

    /**
     * Пользователь вернулся в приложение после перехода на страницу банка.
     * Подтвердить оплату мы не можем (вебхука нет), поэтому формулировка нейтральная.
     */
    fun onReturnedFromPayment() {
        if (_donationState.value is DonationUiState.AwaitingReturn) {
            _donationState.value = DonationUiState.Returned
        }
    }

    fun dismissDonationState() { _donationState.value = DonationUiState.Idle }
}

/** Состояние процесса пожертвования для UI. */
sealed interface DonationUiState {
    data object Idle : DonationUiState
    data object Starting : DonationUiState
    /** Браузер/банк открыт, ждём возвращения пользователя. */
    data object AwaitingReturn : DonationUiState
    /** Пользователь вернулся; статус платежа неизвестен. */
    data object Returned : DonationUiState
    data class Error(val message: String) : DonationUiState
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
    val donationState by viewModel.donationState.collectAsState()
    val suggestionError by viewModel.suggestionError.collectAsState()

    // Пользователь вернулся в приложение со страницы банка. Подтвердить оплату мы
    // не можем (вебхука нет), поэтому просто показываем нейтральное сообщение.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onReturnedFromPayment()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Пояснение ПЕРЕД переходом: платёж происходит вне приложения и его статус
    // приложению неизвестен. Пользователь должен понимать это заранее, а не
    // выяснять постфактум, когда деньги уже ушли.
    var showPayExplain by remember { mutableStateOf(false) }
    if (showPayExplain) {
        AlertDialog(
            onDismissRequest = { showPayExplain = false },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.OpenInNew, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(32.dp)) },
            title = { Text("Переход к оплате", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Сейчас откроется защищённая страница банка в браузере. " +
                            "Сумму вы указываете там же и подтверждаете перевод в своём банке.",
                        color = NexoryColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Оплата проходит вне Nexory: приложение не получает данных о платеже " +
                            "и не отслеживает его статус. Проверить перевод можно в истории операций банка.",
                        color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPayExplain = false; viewModel.donate(context) }) {
                    Text("Продолжить", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayExplain = false }) {
                    Text("Отмена", color = NexoryColors.TextSecondary)
                }
            },
        )
    }

    if (donationState is DonationUiState.Returned) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDonationState() },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Favorite, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Спасибо!", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Если оплата прошла успешно, поддержка проекта уже учтена.\n\n" +
                        "Приложение не получает данных о платеже, поэтому проверить статус " +
                        "перевода можно в истории операций вашего банка.",
                    color = NexoryColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDonationState() }) {
                    Text("Понятно", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

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
                    onValueChange = { suggestion = it; viewModel.clearSuggestionError() },
                    modifier = Modifier.fillMaxWidth().height(120.dp).scrollOnFocus(),
                    placeholder = { Text("Что можно улучшить или добавить?", color = NexoryColors.TextSecondary) },
                    isError = suggestionError != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                // Раньше ошибка отправки проглатывалась и кнопка «ничего не делала»
                suggestionError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = NexoryColors.Error, fontSize = 12.sp)
                }
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

            // 4. Поддержать проект — оплата на стороне банка, реквизиты не раскрываются
            SectionTitle("Поддержать проект")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(16.dp),
            ) {
                Text(
                    "Поддержать проект можно добровольным переводом любой суммы — это ускоряет " +
                        "выход новых функций. Сумму вы вводите на защищённой странице банка, " +
                        "там же подтверждаете перевод.",
                    color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lock, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(14.dp).padding(top = 3.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Никакие платёжные данные не проходят через приложение и нигде в нём не отображаются.",
                        color = NexoryColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                    )
                }

                Spacer(Modifier.height(14.dp))

                val isStarting = donationState is DonationUiState.Starting
                Button(
                    onClick = { showPayExplain = true },
                    enabled = !isStarting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isStarting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Поддержать проект", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Ошибка: нет сети / нет браузера / приём не настроен
                (donationState as? DonationUiState.Error)?.let { err ->
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NexoryColors.Error.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = NexoryColors.Error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(err.message, color = NexoryColors.Error, fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Попробовать снова",
                                color = NexoryColors.PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { viewModel.donate(context) },
                            )
                        }
                    }
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

// openUrl удалён: открытие внешних ссылок централизовано в
// ui/components/UrlOpener.kt и используется через DonationService.
