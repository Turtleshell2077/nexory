package com.nexory.app.ui.screens.events

import android.net.Uri
import android.view.ContextThemeWrapper
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.theme.NexoryColors
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
import java.util.Calendar

val EVENT_CATEGORIES = listOf(
    "Музыка", "Спорт", "Еда", "Искусство", "Игры",
    "Природа", "Образование", "Вечеринка", "Технологии", "Кино"
)

val SKILL_LEVELS = listOf("Любой уровень", "Начинающий", "Средний", "Продвинутый", "Профессионал")
val EVENT_TYPES  = listOf("Встреча", "Тренировка", "Турнир", "Соревнование", "Мастер-класс", "Вечеринка", "Прогулка", "Игра")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    eventId: String? = null,                 // != null → режим редактирования
    viewModel: CreateEventViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val loaded = uiState.loaded
    val isEdit = eventId != null

    LaunchedEffect(eventId) { if (eventId != null) viewModel.loadEvent(eventId) }
    LaunchedEffect(uiState.isCreated) {
        if (uiState.isCreated) navController.popBackStack()
    }

    var title           by remember(loaded) { mutableStateOf(loaded?.title ?: "") }
    var description     by remember(loaded) { mutableStateOf(loaded?.description ?: "") }
    var location        by remember(loaded) { mutableStateOf(loaded?.address ?: "") }
    var category        by remember(loaded) { mutableStateOf(loaded?.category ?: "") }
    var maxParticipants by remember(loaded) { mutableStateOf(loaded?.maxParticipants?.toString() ?: "") }
    var isPrivate       by remember(loaded) { mutableStateOf(loaded?.isPrivate ?: false) }
    var price           by remember(loaded) { mutableStateOf(loaded?.price?.takeIf { it > 0.0 }?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var priceDesc       by remember(loaded) { mutableStateOf(loaded?.priceDescription ?: "") }
    var skillLevel      by remember(loaded) { mutableStateOf(loaded?.skillLevel ?: "") }
    var metro           by remember(loaded) { mutableStateOf(loaded?.metro ?: "") }
    var eventType       by remember(loaded) { mutableStateOf(loaded?.eventType ?: "") }
    var coverUri        by remember { mutableStateOf<Uri?>(null) }   // превью
    var coverUrl        by remember(loaded) { mutableStateOf(loaded?.coverUrl) } // загруженный URL
    var uploadingCover  by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Дата/время: ISO для бэкенда + человекочитаемое для UI
    var startsAtIso     by remember(loaded) { mutableStateOf(loaded?.startsAt ?: "") }
    var endsAtIso       by remember(loaded) { mutableStateOf(loaded?.endsAt) }
    var dateTimeDisplay by remember(loaded) { mutableStateOf(loaded?.let { formatEventDateTime(it.startsAt, it.endsAt) } ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var levelExpanded   by remember { mutableStateOf(false) }
    var typeExpanded    by remember { mutableStateOf(false) }
    var showDateTimeDialog by remember { mutableStateOf(false) }
    var showLimitInfo by remember { mutableStateOf(false) }

    // Кадрирование обложки 16:9
    val cropCover = com.nexory.app.ui.components.rememberImageCropper(aspectX = 16f, aspectY = 9f, circle = false) { cropped ->
        coverUri = cropped
        scope.launch {
            uploadingCover = true
            coverUrl = viewModel.uploadImage(cropped) ?: coverUrl
            uploadingCover = false
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) cropCover(uri) }

    if (showDateTimeDialog) {
        DateTimeWheelDialog(
            onDismiss = { showDateTimeDialog = false },
            onConfirm = { startIso, endIso, display ->
                startsAtIso = startIso
                endsAtIso = endIso
                dateTimeDisplay = display
                showDateTimeDialog = false
            },
        )
    }

    if (showLimitInfo) {
        AlertDialog(
            onDismissRequest = { showLimitInfo = false },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                TextButton(onClick = { showLimitInfo = false }) {
                    Text("Понятно", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            title = { Text("Количество участников", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Если оставить поле пустым — количество участников будет неограниченным.",
                    color = NexoryColors.TextSecondary, fontSize = 14.sp,
                )
            },
        )
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Редактирование" else "Новое мероприятие", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, null, tint = NexoryColors.TextPrimary)
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ---- Фото мероприятия ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoryColors.SurfaceDark)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = coverUri, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    )
                    if (uploadingCover) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Изменить фото", color = Color.White, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = NexoryColors.TextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Добавить фото мероприятия", color = NexoryColors.TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            // ---- Основная информация ----
            FieldLabel("Название")
            PlainField(value = title, onValueChange = { title = it }, placeholder = "Например: Игра в футбол")

            FieldLabel("Описание")
            PlainField(value = description, onValueChange = { description = it }, placeholder = "Расскажи о мероприятии", maxLines = 5)

            // ---- Местоположение ----
            FieldLabel("Местоположение")
            PlainField(value = location, onValueChange = { location = it }, placeholder = "Город, адрес или место встречи")

            // ---- Метро рядом ----
            FieldLabel("Метро рядом")
            com.nexory.app.ui.components.MetroAutocompleteField(value = metro, onChange = { metro = it })

            // ---- Дата и время ----
            FieldLabel("Дата и время")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexoryColors.SurfaceMid)
                    .clickable { showDateTimeDialog = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, null,
                        tint = if (startsAtIso.isBlank()) NexoryColors.TextSecondary else NexoryColors.PrimaryBlue,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        dateTimeDisplay.ifBlank { "Выбрать дату и время" },
                        color = if (startsAtIso.isBlank()) NexoryColors.TextSecondary else NexoryColors.TextPrimary,
                        fontSize = 15.sp,
                    )
                }
            }

            // ---- Категория ----
            FieldLabel("Категория")
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    placeholder = { Text("Выбери категорию", color = NexoryColors.TextSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    EVENT_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = NexoryColors.TextPrimary) },
                            onClick = { category = cat; categoryExpanded = false },
                        )
                    }
                }
            }

            // ---- Тип мероприятия ----
            FieldLabel("Тип мероприятия")
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = eventType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    placeholder = { Text("Например: Турнир", color = NexoryColors.TextSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    EVENT_TYPES.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t, color = NexoryColors.TextPrimary) },
                            onClick = { eventType = t; typeExpanded = false },
                        )
                    }
                }
            }

            // ---- Стоимость ----
            FieldLabel("Стоимость (₽)")
            PlainField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() } },
                placeholder = "0 — бесплатно",
                keyboardType = KeyboardType.Number,
            )
            // Описание стоимости — только если цена задана
            if (price.isNotBlank() && price != "0") {
                FieldLabel("За что оплата")
                PlainField(
                    value = priceDesc,
                    onValueChange = { priceDesc = it },
                    placeholder = "Например: аренда зала, инвентарь",
                    maxLines = 3,
                )
            }

            // ---- Уровень ----
            FieldLabel("Уровень участников")
            ExposedDropdownMenuBox(expanded = levelExpanded, onExpandedChange = { levelExpanded = it }) {
                OutlinedTextField(
                    value = skillLevel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    placeholder = { Text("Не указан", color = NexoryColors.TextSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoryTextFieldColors(),
                )
                ExposedDropdownMenu(expanded = levelExpanded, onDismissRequest = { levelExpanded = false }) {
                    SKILL_LEVELS.forEach { lvl ->
                        DropdownMenuItem(
                            text = { Text(lvl, color = NexoryColors.TextPrimary) },
                            onClick = { skillLevel = if (lvl == "Любой уровень") "" else lvl; levelExpanded = false },
                        )
                    }
                }
            }

            // ---- Количество участников ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("Количество участников")
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(NexoryColors.SurfaceMid, androidx.compose.foundation.shape.CircleShape)
                        .clickable { showLimitInfo = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.QuestionMark, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(11.dp))
                }
            }
            PlainField(
                value = maxParticipants,
                onValueChange = { maxParticipants = it.filter { c -> c.isDigit() } },
                placeholder = "Количество участников",
                keyboardType = KeyboardType.Number,
            )

            // ---- Приватность ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexoryColors.SurfaceDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Приватное мероприятие", color = NexoryColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Видно только друзьям", color = NexoryColors.TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NexoryColors.Violet,
                        uncheckedTrackColor = NexoryColors.SurfaceMid,
                    )
                )
            }

            uiState.error?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexoryColors.Error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Error, null, tint = NexoryColors.Error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = NexoryColors.Error, fontSize = 13.sp)
                }
            }

            // ---- Кнопка сохранения ----
            val canCreate = !uiState.isLoading && title.isNotBlank() && location.isNotBlank() && startsAtIso.isNotBlank()
            Button(
                onClick = {
                    viewModel.save(
                        eventId         = eventId,
                        title           = title,
                        description     = description.takeIf { it.isNotBlank() },
                        address         = location,
                        category        = category.takeIf { it.isNotBlank() },
                        startsAt        = startsAtIso,
                        endsAt          = endsAtIso,
                        maxParticipants = maxParticipants.toIntOrNull(),
                        isPrivate       = isPrivate,
                        coverUrl        = coverUrl,
                        price           = price.toDoubleOrNull(),
                        skillLevel      = skillLevel.takeIf { it.isNotBlank() },
                        eventType       = eventType.takeIf { it.isNotBlank() },
                        priceDescription = priceDesc.takeIf { it.isNotBlank() },
                        metro           = metro.takeIf { it.isNotBlank() },
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = canCreate,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        if (canCreate) Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                        else Brush.linearGradient(listOf(NexoryColors.SurfaceMid, NexoryColors.SurfaceMid))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(if (isEdit) "Сохранить изменения" else "Создать мероприятие", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        modifier        = Modifier.fillMaxWidth(),
        placeholder     = { Text(placeholder, color = NexoryColors.TextSecondary) },
        maxLines        = maxLines,
        singleLine      = maxLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape           = RoundedCornerShape(12.dp),
        colors          = nexoryTextFieldColors(),
    )
}

// Двухшаговый wheel-пикер: сначала прокручиваемая дата (год/месяц/день),
// затем стрелка "Далее" → выбор времени начала и окончания.
@Composable
fun DateTimeWheelDialog(
    onDismiss: () -> Unit,
    onConfirm: (startIso: String, endIso: String?, display: String) -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    var step by remember { mutableStateOf(0) } // 0 = дата, 1 = время

    var year  by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(now.get(Calendar.MONTH)) }
    var day   by remember { mutableStateOf(now.get(Calendar.DAY_OF_MONTH)) }

    var startHour by remember { mutableStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var startMin  by remember { mutableStateOf(0) }
    var endHour   by remember { mutableStateOf((now.get(Calendar.HOUR_OF_DAY) + 1) % 24) }
    var endMin    by remember { mutableStateOf(0) }

    fun pad(n: Int) = n.toString().padStart(2, '0')
    fun iso(h: Int, m: Int) = "$year-${pad(month + 1)}-${pad(day)}T${pad(h)}:${pad(m)}:00"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoryColors.SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (step == 0) "Выбери дату" else "Время мероприятия",
                color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (step == 0) {
                // Прокручиваемый spinner-календарь (Holo-тема даёт «барабаны»)
                AndroidView(
                    factory = { ctx ->
                        val themed = ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Dialog)
                        DatePicker(themed).apply {
                            calendarViewShown = false
                            minDate = System.currentTimeMillis() - 1000
                            init(year, month, day) { _, y, m, d ->
                                year = y; month = m; day = d
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column {
                    Text("Начало", color = NexoryColors.TextSecondary, fontSize = 13.sp)
                    AndroidView(
                        factory = { ctx ->
                            TimePicker(ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Dialog)).apply {
                                setIs24HourView(true)
                                hour = startHour; minute = startMin
                                setOnTimeChangedListener { _, h, m -> startHour = h; startMin = m }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Окончание", color = NexoryColors.TextSecondary, fontSize = 13.sp)
                    AndroidView(
                        factory = { ctx ->
                            TimePicker(ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Dialog)).apply {
                                setIs24HourView(true)
                                hour = endHour; minute = endMin
                                setOnTimeChangedListener { _, h, m -> endHour = h; endMin = m }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (step == 0) {
                TextButton(onClick = { step = 1 }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Далее", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ArrowForward, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                TextButton(onClick = {
                    val months = listOf("янв","фев","мар","апр","мая","июн","июл","авг","сен","окт","ноя","дек")
                    val display = "$day ${months[month]} $year, ${pad(startHour)}:${pad(startMin)}–${pad(endHour)}:${pad(endMin)}"
                    onConfirm(iso(startHour, startMin), iso(endHour, endMin), display)
                }) {
                    Text("Готово", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (step == 1) step = 0 else onDismiss() }) {
                Text(if (step == 1) "Назад" else "Отмена", color = NexoryColors.TextSecondary)
            }
        },
    )
}
