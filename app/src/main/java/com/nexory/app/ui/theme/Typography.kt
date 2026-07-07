package com.nexory.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NexoryTypography = Typography(
    // Крупные заголовки (название мероприятия на детальном экране)
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 34.sp,
        color      = NexoryColors.TextPrimary,
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
        color      = NexoryColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        color      = NexoryColors.TextSecondary,
    ),
    // Метки, теги
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        color      = NexoryColors.TextSecondary,
    ),
)