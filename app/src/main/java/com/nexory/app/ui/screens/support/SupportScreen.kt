package com.nexory.app.ui.screens.support

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.theme.NexoryColors
import androidx.compose.material3.ExperimentalMaterial3Api

private val CATEGORIES = listOf(
    Triple(Icons.Default.AccountCircle, "Аккаунт",       NexoryColors.PrimaryBlue),
    Triple(Icons.Default.Event,         "Мероприятия",   NexoryColors.Violet),
    Triple(Icons.Default.Chat,          "Чаты",          NexoryColors.LightViolet),
    Triple(Icons.Default.BugReport,     "Баг / ошибка",  NexoryColors.Error),
)

private val FAQ = listOf(
    "Как создать мероприятие?" to "Нажми кнопку + на главном экране, заполни форму и нажми «Создать мероприятие».",
    "Как добавить друга?" to "Открой профиль пользователя через поиск или из чата и нажми «Добавить в друзья».",
    "Не приходят уведомления?" to "Проверь разрешения в настройках телефона → Nexory → Уведомления.",
    "Как удалить своё мероприятие?" to "Открой мероприятие, нажми ⋮ в правом верхнем углу → «Удалить».",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    navController: NavController,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var subject  by remember { mutableStateOf("") }
    var body     by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var expandedFaq by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.isSent) {
        if (state.isSent) navController.popBackStack()
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text("Поддержка", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ---- Баннер ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NexoryColors.DeepBlue.copy(alpha = 0.8f), NexoryColors.Violet.copy(alpha = 0.6f))
                        )
                    )
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Служба поддержки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Отвечаем в течение 24 часов", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
            }

            // ---- Вступление ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexoryColors.SurfaceDark)
                    .padding(16.dp),
            ) {
                Text("Приложение ещё развивается 🚀", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Мы только в начале пути и будем рады любым предложениям и отзывам. " +
                        "Если заметишь ошибку или баг — расскажи нам, это очень поможет сделать Nexory лучше. Спасибо, что ты с нами!",
                    color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
                )
            }

            // ---- Категория проблемы ----
            SupportSectionLabel("Категория обращения")

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CATEGORIES.forEach { (icon, label, color) ->
                    CategoryChip(
                        icon     = icon,
                        label    = label,
                        color    = color,
                        selected = category == label,
                        onClick  = {
                            val prev = category
                            category = label
                            // Подставляем тему, если поле пустое или совпадало с прошлой категорией
                            if (subject.isBlank() || subject == prev) subject = label
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ---- Форма обращения ----
            SupportSectionLabel("Опишите проблему")

            OutlinedTextField(
                value         = subject,
                onValueChange = { subject = it },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Тема *") },
                leadingIcon   = { Icon(Icons.Default.Subject, null) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = nexoryTextFieldColors(),
            )

            OutlinedTextField(
                value         = body,
                onValueChange = { body = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label         = { Text("Подробное описание *") },
                leadingIcon   = { Icon(Icons.Default.Message, null) },
                maxLines      = 8,
                shape         = RoundedCornerShape(12.dp),
                colors        = nexoryTextFieldColors(),
            )

            state.error?.let {
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

            Button(
                onClick  = { viewModel.send(subject, body) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = subject.isNotBlank() && body.isNotBlank() && !state.isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (subject.isNotBlank() && body.isNotBlank())
                                Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                            else Brush.linearGradient(listOf(NexoryColors.SurfaceMid, NexoryColors.SurfaceMid))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Отправить обращение", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            // ---- FAQ ----
            SupportSectionLabel("Частые вопросы")

            FAQ.forEachIndexed { index, (question, answer) ->
                FaqItem(
                    question = question,
                    answer   = answer,
                    expanded = expandedFaq == index,
                    onClick  = { expandedFaq = if (expandedFaq == index) null else index },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SupportSectionLabel(text: String) {
    Text(
        text,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = NexoryColors.TextSecondary,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun CategoryChip(
    icon:     ImageVector,
    label:    String,
    color:    Color,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else NexoryColors.SurfaceDark)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = if (selected) color else NexoryColors.TextSecondary, modifier = Modifier.size(20.dp))
        Text(
            label,
            fontSize   = 10.sp,
            color      = if (selected) color else NexoryColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun FaqItem(question: String, answer: String, expanded: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexoryColors.SurfaceDark)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.QuestionAnswer,
                null,
                tint     = NexoryColors.PrimaryBlue,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                question,
                color      = NexoryColors.TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = NexoryColors.TextSecondary,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = NexoryColors.SurfaceMid)
            Spacer(Modifier.height(8.dp))
            Text(answer, color = NexoryColors.TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
