package com.nexory.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nexory.app.ui.theme.NexoryColors
import kotlin.random.Random

/**
 * Одно пятно фонового узора. Координаты и радиус — ДОЛИ от размера экрана,
 * а не пиксели: узор одинаково ложится и на маленький телефон, и на планшет.
 */
private data class Speck(
    val x: Float, val y: Float,
    val radius: Float,
    val colorIndex: Int,
    val ring: Boolean,
)

/**
 * Узор рассчитывается ОДИН раз на фиксированном зерне.
 *
 * Фиксированное зерно принципиально: со случайным при каждой рекомпозиции
 * кружки перескакивали бы с места на место — фон дёргался бы при каждом новом
 * сообщении. С фиксированным узор всегда один и тот же, пользователь его
 * запоминает как «обои приложения».
 */
private fun buildPattern(): List<Speck> {
    val rnd = Random(20260728)
    return List(26) {
        Speck(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            // Разброс размеров даёт ощущение глубины: мелкие читаются как дальние
            radius = 0.012f + rnd.nextFloat() * 0.055f,
            colorIndex = rnd.nextInt(4),
            ring = rnd.nextFloat() < 0.45f,
        )
    }
}

/**
 * Фон переписки — «конфетти» в фирменных цветах.
 *
 * Задача была не просто убрать чёрную заливку, а связать экран с тем, ради чего
 * приложение существует: встречи, активность, движение. Отсюда рассыпанные
 * круги и кольца разного размера — читаются как конфетти и огни, но остаются
 * настолько приглушёнными, что не мешают читать сообщения.
 *
 * Слои снизу вверх:
 *  1. диагональный градиент в индиго-фиолетовой гамме — задаёт настроение;
 *  2. два больших размытых пятна по углам — объём;
 *  3. конфетти: круги и кольца фирменных цветов.
 *
 * Цвета берутся из [NexoryColors], поэтому фон переключается вместе с темой.
 * В светлой теме прозрачность ниже: на белом та же насыщенность выглядела бы
 * грязным пятном, а не лёгким узором.
 *
 * Всё рисуется в [drawBehind] — на этапе отрисовки, без рекомпозиции при
 * прокрутке списка сообщений.
 */
@Composable
fun ChatBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark    = NexoryColors.isDark
    val pattern = remember { buildPattern() }

    // Подложка: у тёмной темы уходим в глубокий индиго, у светлой — в лаванду
    val baseTop    = NexoryColors.DeepBlack
    val baseMid    = if (dark) NexoryColors.DeepBlue.copy(alpha = 0.55f)
                     else NexoryColors.Violet.copy(alpha = 0.07f)
    val baseBottom = NexoryColors.SurfaceDark

    val glowBlue   = NexoryColors.PrimaryBlue.copy(alpha = if (dark) 0.20f else 0.10f)
    val glowViolet = NexoryColors.Violet.copy(alpha = if (dark) 0.16f else 0.08f)

    val speckAlpha = if (dark) 0.15f else 0.11f
    val palette = listOf(
        NexoryColors.PrimaryBlue.copy(alpha = speckAlpha),
        NexoryColors.Violet.copy(alpha = speckAlpha),
        NexoryColors.LightViolet.copy(alpha = speckAlpha),
        NexoryColors.GradientEnd.copy(alpha = speckAlpha),
    )

    Box(
        modifier = modifier.drawBehind {
            // 1. Диагональный градиент вместо простого вертикального —
            //    он живее и уводит взгляд по экрану
            drawRect(
                Brush.linearGradient(
                    colors = listOf(baseTop, baseMid, baseBottom),
                    start  = Offset(0f, 0f),
                    end    = Offset(size.width, size.height),
                )
            )

            // 2. Крупные размытые пятна по углам
            val r = size.minDimension * 0.9f
            val topLeft = Offset(size.width * 0.10f, size.height * 0.08f)
            drawCircle(
                brush  = Brush.radialGradient(listOf(glowBlue, Color.Transparent), topLeft, r),
                radius = r, center = topLeft,
            )
            val bottomRight = Offset(size.width * 0.95f, size.height * 0.92f)
            drawCircle(
                brush  = Brush.radialGradient(listOf(glowViolet, Color.Transparent), bottomRight, r),
                radius = r, center = bottomRight,
            )

            // 3. Конфетти
            pattern.forEach { speck ->
                val center = Offset(speck.x * size.width, speck.y * size.height)
                val radius = speck.radius * size.minDimension
                val color  = palette[speck.colorIndex]
                if (speck.ring) {
                    drawCircle(color, radius, center, style = Stroke(width = radius * 0.24f))
                } else {
                    drawCircle(color, radius, center)
                }
            }
        },
        content = content,
    )
}
