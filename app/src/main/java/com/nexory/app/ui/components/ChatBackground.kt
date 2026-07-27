package com.nexory.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nexory.app.ui.theme.NexoryColors

/**
 * Фон переписки.
 *
 * Раньше здесь была ровная чёрная заливка: экран выглядел незаконченным и
 * ничем не отличался от любого другого списка в приложении. Теперь это мягкий
 * вертикальный градиент с двумя приглушёнными пятнами фирменных цветов по
 * диагонали — рисунок заметен ровно настолько, чтобы задать настроение, и не
 * спорит с текстом сообщений.
 *
 * Цвета берутся из [NexoryColors], поэтому фон переключается вместе с темой
 * приложения. В светлой теме подсветка слабее: на белом та же насыщенность
 * читалась бы как грязное пятно, а не как акцент.
 *
 * Рисуем через [drawBehind] — операция идёт на этапе отрисовки и не вызывает
 * рекомпозицию при прокрутке списка сообщений.
 */
@Composable
fun ChatBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark      = NexoryColors.isDark
    val top       = NexoryColors.DeepBlack
    val bottom    = NexoryColors.SurfaceDark
    val glowBlue  = NexoryColors.PrimaryBlue.copy(alpha = if (dark) 0.16f else 0.09f)
    val glowViolet = NexoryColors.Violet.copy(alpha = if (dark) 0.13f else 0.07f)

    Box(
        modifier = modifier.drawBehind {
            drawRect(Brush.verticalGradient(listOf(top, bottom)))

            val r = size.minDimension * 0.85f
            val topLeft = Offset(size.width * 0.12f, size.height * 0.10f)
            drawCircle(
                brush  = Brush.radialGradient(listOf(glowBlue, Color.Transparent), topLeft, r),
                radius = r, center = topLeft,
            )
            val bottomRight = Offset(size.width * 0.92f, size.height * 0.88f)
            drawCircle(
                brush  = Brush.radialGradient(listOf(glowViolet, Color.Transparent), bottomRight, r),
                radius = r, center = bottomRight,
            )
        },
        content = content,
    )
}
