package com.nexory.app.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nexory.app.data.local.SettingsManager
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsManager,
) : ViewModel() {
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch { settings.setOnboardingDone(); onDone() }
    }
}

private data class Slide(val icon: ImageVector, val title: String, val text: String)

private val SLIDES = listOf(
    Slide(Icons.Default.Explore, "Добро пожаловать в Nexory",
        "Находи мероприятия рядом, знакомься с людьми по интересам и создавай свои события."),
    Slide(Icons.Default.Groups, "События и компания",
        "Спорт, игры, встречи, мастер-классы — выбирай, записывайся и зови друзей."),
    Slide(Icons.Default.Chat, "Общение и чаты",
        "Чат каждого мероприятия и личные сообщения. Оставайся на связи с участниками."),
    Slide(Icons.Default.Verified, "Всё под контролем",
        "Управляй профилем, приватностью и уведомлениями. Погнали!"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState { SLIDES.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == SLIDES.lastIndex

    fun done() = viewModel.finish {
        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(NexoryColors.DeepBlack).padding(24.dp),
    ) {
        // Пропустить
        Box(Modifier.fillMaxWidth()) {
            TextButton(onClick = { done() }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text("Пропустить", color = NexoryColors.TextSecondary)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val s = SLIDES[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(s.icon, null, tint = Color.White, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(s.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(s.text, fontSize = 15.sp, color = NexoryColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        }

        // Индикаторы
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            SLIDES.indices.forEach { i ->
                val active = i == pagerState.currentPage
                val width by animateDpAsState(if (active) 24.dp else 8.dp, label = "dot")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (active) NexoryColors.PrimaryBlue else NexoryColors.SurfaceMid),
                )
            }
        }

        // Кнопка
        Button(
            onClick = {
                if (isLast) done()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isLast) "Начать" else "Далее", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
