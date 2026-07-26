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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Аватар-заглушка: фон по выбранному шаблону плюс инициалы.
 *
 * Рисуется на Canvas, а не картинкой: масштабируется без потери качества,
 * ничего не грузит по сети и не занимает места на диске. Тот же компонент
 * используется и в списках, и в превью на экранах выбора — поэтому выбранный
 * вариант выглядит ровно так же, как потом в приложении.
 *
 * @param seed стабильный ключ (id пользователя) — от него зависит расстановка
 *             фигур в геометрическом шаблоне, чтобы у каждого получился свой узор.
 */
@Composable
fun GeneratedAvatar(
    selection: AvatarPresets.Selection,
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier,
    seed: String = initials,
) {
    val palette = AvatarPresets.paletteAt(selection.variant)
    val hash = AvatarPresets.hashOf(seed)

    // У дуотона инициалы крупнее: он на них и построен
    val textScale = if (selection.style == AvatarPresets.Style.DUOTONE) 0.46f else 0.36f

    Box(
        modifier = modifier.size(size).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            when (selection.style) {
                AvatarPresets.Style.GRADIENT  -> drawGradient(palette)
                AvatarPresets.Style.DUOTONE   -> drawDuotone(palette)
                AvatarPresets.Style.GEOMETRIC -> drawGeometric(palette, hash)
                AvatarPresets.Style.WAVES     -> drawWaves(palette)
                AvatarPresets.Style.TEXTURE   -> drawTexture(palette)
            }
        }
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * textScale).sp,
        )
    }
}

// ---- Шаблоны ----

/** Плавный переход трёх оттенков по диагонали. */
private fun DrawScope.drawGradient(palette: List<Color>) {
    drawRect(
        Brush.linearGradient(
            colors = palette,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        )
    )
}

/** Два оттенка, разделённые мягкой диагональю — глубже плоской заливки. */
private fun DrawScope.drawDuotone(palette: List<Color>) {
    drawRect(palette[0])
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.35f)
        // Лёгкий изгиб вместо прямой линии — выглядит мягче
        quadraticBezierTo(size.width * 0.5f, size.height * 0.15f, size.width, size.height * 0.55f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, palette[1])
}

/**
 * Абстрактный узор: круги и дуги, положение которых выводится из хэша id.
 * У каждого пользователя получается свой, но всегда одинаковый рисунок.
 */
private fun DrawScope.drawGeometric(palette: List<Color>, hash: Int) {
    drawRect(Brush.linearGradient(listOf(palette[0], palette[1])))

    val accent = palette[2].copy(alpha = 0.55f)
    val soft = Color.White.copy(alpha = 0.18f)
    val s = size.minDimension

    // Три круга разного размера в позициях, зависящих от хэша
    val positions = listOf(
        Offset(s * (0.18f + (hash % 5) * 0.06f), s * 0.24f),
        Offset(s * (0.62f + ((hash / 7) % 4) * 0.05f), s * 0.70f),
        Offset(s * 0.80f, s * (0.18f + ((hash / 13) % 4) * 0.05f)),
    )
    val radii = listOf(s * 0.20f, s * 0.15f, s * 0.10f)
    positions.forEachIndexed { i, p ->
        drawCircle(color = if (i % 2 == 0) accent else soft, radius = radii[i], center = p)
    }
    // Дуга-акцент
    drawCircle(
        color = Color.White.copy(alpha = 0.22f),
        radius = s * 0.42f,
        center = Offset(s * 0.5f, s * 0.5f),
        style = Stroke(width = s * 0.035f),
    )
}

/** Мягкие перетекающие формы поверх тонированного фона. */
private fun DrawScope.drawWaves(palette: List<Color>) {
    drawRect(Brush.verticalGradient(listOf(palette[0], palette[1])))

    // Две волны разной высоты и прозрачности — создаёт ощущение глубины
    fun wave(yFactor: Float, color: Color, amplitude: Float) {
        val path = Path().apply {
            moveTo(0f, size.height * yFactor)
            cubicTo(
                size.width * 0.25f, size.height * (yFactor - amplitude),
                size.width * 0.75f, size.height * (yFactor + amplitude),
                size.width, size.height * yFactor,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
    }
    wave(0.62f, palette[2].copy(alpha = 0.45f), 0.14f)
    wave(0.78f, Color.White.copy(alpha = 0.16f), 0.10f)
}

/** Тонкая диагональная сетка поверх ровного фона — фактура без визуального шума. */
private fun DrawScope.drawTexture(palette: List<Color>) {
    drawRect(Brush.linearGradient(listOf(palette[1], palette[0])))

    val ink = Color.White.copy(alpha = 0.14f)
    val step = size.minDimension / 9f
    val stroke = size.minDimension * 0.02f

    // Сетка под 45° — на маленьком размере читается как приятная фактура
    rotate(degrees = 45f) {
        var x = -size.width
        while (x < size.width * 2) {
            drawLine(ink, Offset(x, -size.height), Offset(x, size.height * 2), strokeWidth = stroke)
            x += step
        }
        var y = -size.height
        while (y < size.height * 2) {
            drawLine(ink, Offset(-size.width, y), Offset(size.width * 2, y), strokeWidth = stroke)
            y += step
        }
    }
}
