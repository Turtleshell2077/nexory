package com.nexory.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Сгенерированный аватар: цветной фон с узором по выбранному стилю и инициалами.
 *
 * Рисуется на Canvas, а не картинкой — поэтому масштабируется без потери качества
 * и не требует ни загрузки по сети, ни места на диске. Один и тот же компонент
 * используется и в [UserAvatar], и в превью на экране выбора, чтобы выбранный
 * вариант выглядел ровно так же, как потом в списках.
 */
@Composable
fun GeneratedAvatar(
    selection: AvatarPresets.Selection,
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = AvatarPresets.paletteAt(selection.variant)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Фон одинаков у всех стилей — различается наложенный узор
            drawRect(Brush.linearGradient(colors), size = this.size)
            when (selection.style) {
                AvatarPresets.Style.GRADIENT -> Unit // чистый градиент
                AvatarPresets.Style.RINGS    -> drawRings()
                AvatarPresets.Style.DOTS     -> drawDots()
                AvatarPresets.Style.STRIPES  -> drawStripes()
                AvatarPresets.Style.BLOCKS   -> drawBlocks()
                AvatarPresets.Style.BURST    -> drawBurst()
            }
        }
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
        )
    }
}

// Узоры рисуем полупрозрачным белым поверх градиента: так они читаются
// на любой палитре и не спорят с цветом инициалов.
private val Ink = Color.White.copy(alpha = 0.22f)

private fun DrawScope.drawRings() {
    val step = size.minDimension / 7f
    for (i in 1..3) {
        drawCircle(
            color = Ink,
            radius = step * i,
            center = center,
            style = Stroke(width = size.minDimension * 0.055f),
        )
    }
}

private fun DrawScope.drawDots() {
    val r = size.minDimension / 18f
    val step = size.minDimension / 4.5f
    var y = step / 2
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) step / 2 else step
        while (x < size.width) {
            drawCircle(color = Ink, radius = r, center = Offset(x, y))
            x += step
        }
        y += step
        row++
    }
}

private fun DrawScope.drawStripes() {
    val w = size.minDimension * 0.10f
    val gap = w * 2.1f
    var x = -size.height
    while (x < size.width + size.height) {
        // Диагональные полосы: сдвигаем верх и низ, получаем наклон 45°
        val path = Path().apply {
            moveTo(x, size.height)
            lineTo(x + size.height, 0f)
            lineTo(x + size.height + w, 0f)
            lineTo(x + w, size.height)
            close()
        }
        drawPath(path, Ink)
        x += gap
    }
}

private fun DrawScope.drawBlocks() {
    val half = size.minDimension / 2f
    // Шахматка из двух четвертей — простой и хорошо узнаваемый рисунок
    drawRect(color = Ink, topLeft = Offset(0f, 0f), size = Size(half, half))
    drawRect(color = Ink, topLeft = Offset(half, half), size = Size(half, half))
}

private fun DrawScope.drawBurst() {
    val rays = 10
    val radius = size.minDimension
    repeat(rays) { i ->
        val start = (2.0 * Math.PI * i / rays)
        val end = start + (Math.PI / rays)
        val path = Path().apply {
            moveTo(center.x, center.y)
            lineTo(
                center.x + (radius * kotlin.math.cos(start)).toFloat(),
                center.y + (radius * kotlin.math.sin(start)).toFloat(),
            )
            lineTo(
                center.x + (radius * kotlin.math.cos(end)).toFloat(),
                center.y + (radius * kotlin.math.sin(end)).toFloat(),
            )
            close()
        }
        drawPath(path, Ink)
    }
}
