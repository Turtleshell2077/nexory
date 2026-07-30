package com.nexory.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.components.scrollOnFocus
import com.nexory.app.ui.theme.NexoryColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.launch

// Объединённый список увлечений: виды спорта + виды деятельности
val INTERESTS = listOf(
    "Футбол", "Баскетбол", "Волейбол", "Теннис", "Бег", "Плавание",
    "Велоспорт", "Йога", "Фитнес", "Бокс", "Скейтбординг", "Горные лыжи",
    "Боевые искусства", "Гольф", "Туризм", "Тренировки", "Соревнования",
    "Поход в горы", "Танцы", "Шахматы", "Киберспорт", "Рыбалка"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navController.popBackStack()
    }

    var username    by remember(state.user) { mutableStateOf(state.user?.username ?: "") }
    var displayName by remember(state.user) { mutableStateOf(state.user?.displayName ?: "") }
    var bio         by remember(state.user) { mutableStateOf(state.user?.bio ?: "") }
    var status      by remember(state.user) { mutableStateOf(state.user?.status ?: "") }
    var avatarUrl   by remember(state.user) { mutableStateOf(state.user?.avatarUrl ?: "") }
    var age         by remember(state.user) { mutableStateOf(state.user?.age?.toString() ?: "") }
    var city        by remember(state.user) { mutableStateOf(state.user?.city ?: "") }
    var phone       by remember(state.user) { mutableStateOf(state.user?.phone ?: "") }

    // Увлечения как список выбранных чипов
    val interests = remember(state.user) {
        mutableStateListOf<String>().apply {
            state.user?.sports?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.let { addAll(it) }
        }
    }
    var interestQuery by remember { mutableStateOf("") }

    var previewUri  by remember { mutableStateOf<Uri?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showInterestsInfo by remember { mutableStateOf(false) }

    if (showInterestsInfo) {
        AlertDialog(
            onDismissRequest = { showInterestsInfo = false },
            containerColor   = NexoryColors.SurfaceDark,
            shape            = RoundedCornerShape(16.dp),
            title  = { Text("Чем увлекаешься", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text   = {
                Text(
                    "Укажи любимые виды спорта, игры и занятия — то, чем ты увлекаешься и чем хотел бы " +
                        "заниматься с другими. По этим увлечениям тебе будут подбираться мероприятия и участники.",
                    color = NexoryColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showInterestsInfo = false }) { Text("Понятно", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold) }
            },
        )
    }
    val scope = rememberCoroutineScope()

    // Кадрирование (круглое) → загрузка.
    // Ошибку обязательно показываем: раньше при неудачной загрузке avatarUrl молча
    // оставался прежним, сохранялся старый URL — и выглядело так, будто фото
    // «не меняется» без каких-либо объяснений.
    val cropAvatar = com.nexory.app.ui.components.rememberImageCropper(circle = true) { cropped ->
        previewUri = cropped
        scope.launch {
            uploadingAvatar = true
            avatarError = null
            when (val result = viewModel.uploadImageWithResult(cropped)) {
                is com.nexory.app.data.network.UploadResult.Success -> {
                    avatarUrl = result.url
                }
                is com.nexory.app.data.network.UploadResult.Failure -> {
                    previewUri = null          // убираем превью, которое не загрузилось
                    avatarError = result.message
                }
            }
            uploadingAvatar = false
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) cropAvatar(uri) }

    fun saveAll() {
        viewModel.save(
            username    = username,
            displayName = displayName,
            bio         = bio,
            status      = status,
            avatarUrl   = avatarUrl,
            age         = age.toIntOrNull(),
            city        = city,
            interests   = interests.joinToString(", "),
            phone       = phone,
        )
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Редактировать профиль", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { saveAll() }, enabled = !state.isLoading) {
                        Text("Сохранить", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ---- Аватар ----
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Пока фото загружается — показываем локальное превью,
                    // иначе UserAvatar (он умеет и фото, и пресеты, и заглушку с инициалами)
                    if (previewUri != null) {
                        AsyncImage(
                            model              = previewUri,
                            contentDescription = "Аватар",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(NexoryColors.SurfaceMid)
                                .clickable { showAvatarDialog = true },
                        )
                    } else {
                        com.nexory.app.ui.components.UserAvatar(
                            url  = avatarUrl.ifBlank { null },
                            name = displayName.ifBlank { username },
                            seed = state.user?.id,
                            size = 100.dp,
                            modifier = Modifier.clickable { showAvatarDialog = true },
                        )
                    }
                    if (uploadingAvatar) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Black.copy(0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd)),
                                CircleShape,
                            )
                            .clickable { showAvatarDialog = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Ошибка загрузки фото — конкретная причина вместо молчаливого «ничего не произошло»
            avatarError?.let { err ->
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
                        Text(err, color = NexoryColors.Error, fontSize = 12.sp, lineHeight = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Выбрать другое фото",
                            color = NexoryColors.PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { avatarError = null; imagePicker.launch("image/*") },
                        )
                    }
                }
            }

            // ---- Поля ----
            ProfileLabel("Никнейм")
            ProfileField(value = username, onValueChange = { username = it }, placeholder = "никнейм")

            ProfileLabel("Имя")
            ProfileField(value = displayName, onValueChange = { displayName = it }, placeholder = "Как тебя зовут")

            // Короткая строка — показывается на профиле сразу под аватаром
            ProfileLabel("Статус")
            ProfileField(
                value = status,
                onValueChange = { if (it.length <= 120) status = it },
                placeholder = "Коротко о себе — видно под аватаром",
            )

            ProfileLabel("О себе")
            ProfileField(value = bio, onValueChange = { bio = it }, placeholder = "Расскажи о себе и где живёшь", maxLines = 4)

            ProfileLabel("Возраст")
            ProfileField(
                value = age,
                onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                placeholder = "Возраст",
                keyboardType = KeyboardType.Number,
            )

            ProfileLabel("Город")
            ProfileField(value = city, onValueChange = { city = it }, placeholder = "Город проживания")

            ProfileLabel("Телефон")
            ProfileField(value = phone, onValueChange = { phone = it }, placeholder = "+7 …", keyboardType = KeyboardType.Phone)

            // ---- Увлечения ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileLabel("Чем увлекаешься")
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(NexoryColors.SurfaceMid, CircleShape)
                        .clickable { showInterestsInfo = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.QuestionMark, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(11.dp))
                }
            }
            // Автоподбор через общий компонент: список рисуется всплывающим окном
            // НАД полем и на разметку не влияет, поэтому поле с набранным текстом
            // всегда остаётся на экране и ничего не смещается.
            val interestSuggestions = remember(interestQuery, interests.size) {
                val q = interestQuery.trim()
                if (q.isBlank()) emptyList()
                else INTERESTS.filter { it.contains(q, ignoreCase = true) && it !in interests }.take(6)
            }
            com.nexory.app.ui.components.AutocompleteTextField(
                value = interestQuery,
                onValueChange = { interestQuery = it },
                suggestions = interestSuggestions,
                onSuggestionPick = { picked ->
                    if (picked !in interests) interests.add(picked)
                    interestQuery = ""
                },
                onCustomValueAdd = { custom ->
                    if (interests.none { it.equals(custom, ignoreCase = true) }) interests.add(custom)
                    interestQuery = ""
                },
                placeholder = "Начни вводить увлечение",
                allowCustomValue = true,
            )

            // Выбранные увлечения чипами
            if (interests.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    interests.forEach { item ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(NexoryColors.PrimaryBlue.copy(alpha = 0.2f))
                                .clickable { interests.remove(item) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item, color = NexoryColors.PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Close, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            state.error?.let { Text(it, color = NexoryColors.Error, fontSize = 13.sp) }

            Spacer(Modifier.height(4.dp))

            // Кнопка сохранения
            Button(
                onClick = { saveAll() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Сохранить изменения", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                }
            }

            // ---- Безопасность ----
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = NexoryColors.SurfaceMid)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сменить пароль", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPasswordDialog) {
        PasswordChangeDialog(
            email          = state.user?.email ?: "",
            knownLoading   = state.passwordLoading,
            knownMessage   = state.passwordMessage,
            knownError     = state.passwordError,
            resetLoading   = state.resetLoading,
            resetCodeSent  = state.resetCodeSent,
            resetMessage   = state.resetMessage,
            resetError     = state.resetError,
            onChangeKnown  = { old, new -> viewModel.changePassword(old, new) },
            onRequestReset = { viewModel.requestPasswordReset() },
            onResetWithCode = { code, new -> viewModel.resetPasswordWithCode(code, new) },
            onClose        = { showPasswordDialog = false },
        )
    }

    // Выбор аватара: галерея, готовые варианты оформления, удаление.
    // Раньше здесь был диалог с полем «вставь URL» — для обычного пользователя
    // это бессмысленно, а удалить фото было нельзя вовсе.
    if (showAvatarDialog) {
        com.nexory.app.ui.components.AvatarPickerSheet(
            currentUrl = avatarUrl.ifBlank { null },
            userName = displayName.ifBlank { username },
            onPickPhoto = { imagePicker.launch("image/*") },
            onRemove = {
                previewUri = null
                // Пустая строка = «удалить» для бэкенда (null означал бы «не менять»)
                avatarUrl = ""
                avatarError = null
            },
            onDismiss = { showAvatarDialog = false },
        )
    }
}

@Composable
private fun PasswordChangeDialog(
    email: String,
    knownLoading: Boolean,
    knownMessage: String?,
    knownError: String?,
    resetLoading: Boolean,
    resetCodeSent: Boolean,
    resetMessage: String?,
    resetError: String?,
    onChangeKnown: (String, String) -> Unit,
    onRequestReset: () -> Unit,
    onResetWithCode: (String, String) -> Unit,
    onClose: () -> Unit,
) {
    // mode: 0 = выбор, 1 = знаю пароль, 2 = через почту
    var mode by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor   = NexoryColors.SurfaceDark,
        shape            = RoundedCornerShape(20.dp),
        title = { Text("Смена пароля", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            when (mode) {
                0 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OptionRow(Icons.Default.Password, "Знаю текущий пароль", "Ввести старый и новый пароль") { mode = 1 }
                    OptionRow(Icons.Default.Email, "Сбросить через почту", "Получить код на $email") { mode = 2 }
                }
                1 -> {
                    var oldPass by remember { mutableStateOf("") }
                    var newPass by remember { mutableStateOf("") }
                    var confirm by remember { mutableStateOf("") }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PasswordField(oldPass, { oldPass = it }, "Текущий пароль")
                        PasswordField(newPass, { newPass = it }, "Новый пароль (мин. 8)")
                        PasswordField(confirm, { confirm = it }, "Повтори новый пароль")
                        knownMessage?.let { Text(it, color = NexoryColors.PrimaryBlue, fontSize = 13.sp) }
                        knownError?.let { Text(it, color = NexoryColors.Error, fontSize = 13.sp) }
                        if (confirm.isNotBlank() && newPass != confirm)
                            Text("Пароли не совпадают", color = NexoryColors.Error, fontSize = 12.sp)
                        val can = oldPass.isNotBlank() && newPass.length >= 8 && newPass == confirm && !knownLoading
                        Button(
                            onClick = { onChangeKnown(oldPass, newPass) },
                            enabled = can,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                        ) {
                            if (knownLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Сменить")
                        }
                    }
                }
                2 -> {
                    var code by remember { mutableStateOf("") }
                    var newPass by remember { mutableStateOf("") }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Код будет отправлен на $email", color = NexoryColors.TextSecondary, fontSize = 13.sp)
                        OutlinedButton(
                            onClick = onRequestReset,
                            enabled = !resetLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoryColors.PrimaryBlue),
                        ) { Text(if (resetCodeSent) "Отправить код повторно" else "Отправить код") }

                        if (resetCodeSent) {
                            ProfileField(code, { code = it }, "Код из письма", keyboardType = KeyboardType.Number)
                            PasswordField(newPass, { newPass = it }, "Новый пароль (мин. 8)")
                            Button(
                                onClick = { onResetWithCode(code, newPass) },
                                enabled = code.isNotBlank() && newPass.length >= 8 && !resetLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                            ) {
                                if (resetLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Изменить пароль")
                            }
                        }
                        resetMessage?.let { Text(it, color = NexoryColors.PrimaryBlue, fontSize = 13.sp) }
                        resetError?.let { Text(it, color = NexoryColors.Error, fontSize = 13.sp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Закрыть", color = NexoryColors.PrimaryBlue) }
        },
        dismissButton = if (mode != 0) {{
            TextButton(onClick = { mode = 0 }) { Text("Назад", color = NexoryColors.TextSecondary) }
        }} else null,
    )
}

@Composable
private fun OptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexoryColors.SurfaceMid)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NexoryColors.PrimaryBlue)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NexoryColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = NexoryColors.TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = NexoryColors.TextSecondary)
    }
}

@Composable
private fun ProfileLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextPrimary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        // scrollOnFocus — поле само поднимается над клавиатурой при фокусе
        modifier        = Modifier.fillMaxWidth().scrollOnFocus(),
        placeholder     = { Text(placeholder, color = NexoryColors.TextSecondary) },
        maxLines        = maxLines,
        singleLine      = maxLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape           = RoundedCornerShape(12.dp),
        colors          = nexoryTextFieldColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        modifier             = Modifier.fillMaxWidth(),
        placeholder          = { Text(placeholder, color = NexoryColors.TextSecondary) },
        singleLine           = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape                = RoundedCornerShape(12.dp),
        colors               = nexoryTextFieldColors(),
    )
}
