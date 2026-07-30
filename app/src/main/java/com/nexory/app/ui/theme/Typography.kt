package com.nexory.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика приложения — ТОЛЬКО размеры и начертания, без цветов.
 *
 * ВАЖНО, почему цвета здесь запрещены. Это обычное top-level `val`: оно
 * вычисляется один раз при загрузке класса. Раньше стили задавали
 * `color = NexoryColors.TextPrimary`, и в них навсегда «запекались» тёмные
 * значения, действовавшие в тот момент. Смена темы на светлую их уже не
 * трогала — весь текст, отрисованный стилями темы, оставался почти белым,
 * то есть невидимым на светлом фоне.
 *
 * Без явного цвета Material берёт его из `LocalContentColor` (onSurface /
 * onBackground цветовой схемы), а схема пересобирается при каждой смене темы.
 */
val NexoryTypography = Typography(
    // Крупные заголовки (название мероприятия на детальном экране)
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
    ),
    // Заголовки карточек
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
    ),
    // Основной текст
    bodyLarge = TextStyle(
        fontSize   = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize   = 14.sp,
        lineHeight = 20.sp,
    ),
    // Метки, теги
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
    ),
)
