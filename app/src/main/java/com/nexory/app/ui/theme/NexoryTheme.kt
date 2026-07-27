package com.nexory.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Палитра. Брендовые цвета постоянны, фон/поверхности/текст — реактивные (mutableStateOf),
// поэтому при смене темы перерисовывается ВЕСЬ интерфейс без правок экранов.
object NexoryColors {
    // ---- Брендовые (одинаковы в светлой и тёмной теме) ----
    //
    // Акцент намеренно смещён из «телеграмного» голубого (#4A90E2 — почти тот же
    // оттенок, что у Telegram) в индиго-фиолетовую зону. Это главный источник
    // визуального сходства: пользователь узнаёт мессенджер именно по цвету акцента.
    // Индиго сохраняет преемственность с фирменным градиентом «синий → фиолетовый»,
    // но читается как отдельный бренд, а не как клон.
    // Имя PrimaryBlue оставлено, чтобы не переписывать сотни мест использования.
    val PrimaryBlue   = Color(0xFF5B62F0)   // индиго — основной акцент
    val DeepBlue      = Color(0xFF272B63)   // глубокий индиго для подложек
    val Violet        = Color(0xFF7B4FE0)
    val LightViolet   = Color(0xFFAA80FF)
    val GradientStart = Color(0xFF5B62F0)
    val GradientEnd   = Color(0xFF8B49E8)
    val Error         = Color(0xFFE25A5A)

    // ---- Зависят от темы (реактивные) ----
    /** Текущая тема. Нужна там, где одного цвета мало — например, фон чата
     *  подбирает подсветку разной насыщенности для тёмного и светлого оформления. */
    var isDark: Boolean by mutableStateOf(true); private set
    var DeepBlack:     Color by mutableStateOf(Color(0xFF0A0A12)); private set
    var SurfaceDark:   Color by mutableStateOf(Color(0xFF12121F)); private set
    var SurfaceMid:    Color by mutableStateOf(Color(0xFF1E1E30)); private set
    var TextPrimary:   Color by mutableStateOf(Color(0xFFF0F0F8)); private set
    var TextSecondary: Color by mutableStateOf(Color(0xFF8888AA)); private set

    fun apply(dark: Boolean) {
        isDark = dark
        if (dark) {
            DeepBlack     = Color(0xFF0A0A12)
            SurfaceDark   = Color(0xFF12121F)
            SurfaceMid    = Color(0xFF1E1E30)
            TextPrimary   = Color(0xFFF0F0F8)
            TextSecondary = Color(0xFF8888AA)
        } else {
            DeepBlack     = Color(0xFFF4F5FA)  // фон
            SurfaceDark   = Color(0xFFFFFFFF)  // карточки/панели
            SurfaceMid    = Color(0xFFE9E9F2)  // поля ввода
            TextPrimary   = Color(0xFF14141C)
            TextSecondary = Color(0xFF6B6B82)
        }
    }
}

private fun darkScheme() = darkColorScheme(
    primary        = NexoryColors.PrimaryBlue,
    secondary      = NexoryColors.Violet,
    tertiary       = NexoryColors.LightViolet,
    background     = NexoryColors.DeepBlack,
    surface        = NexoryColors.SurfaceDark,
    surfaceVariant = NexoryColors.SurfaceMid,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = NexoryColors.TextPrimary,
    onSurface      = NexoryColors.TextPrimary,
    error          = NexoryColors.Error,
)

private fun lightScheme() = lightColorScheme(
    primary        = NexoryColors.PrimaryBlue,
    secondary      = NexoryColors.Violet,
    tertiary       = NexoryColors.LightViolet,
    background     = NexoryColors.DeepBlack,
    surface        = NexoryColors.SurfaceDark,
    surfaceVariant = NexoryColors.SurfaceMid,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = NexoryColors.TextPrimary,
    onSurface      = NexoryColors.TextPrimary,
    error          = NexoryColors.Error,
)

@Composable
fun NexoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Применяем палитру в начале композиции — родитель пишет до того, как дети читают.
    NexoryColors.apply(darkTheme)

    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme() else lightScheme(),
        typography  = NexoryTypography,
        content     = content,
    )
}
