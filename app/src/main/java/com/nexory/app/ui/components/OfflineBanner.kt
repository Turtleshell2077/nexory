package com.nexory.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexory.app.ui.theme.NexoryColors

/**
 * Баннер отсутствия сети.
 *
 * Требование раздела «оффлайн-режим»: пользователь должен явно понимать, что видит
 * сохранённые данные, а не актуальные. Без этого возникает худший сценарий — человек
 * считает устаревшую ленту актуальной и приходит на отменённое мероприятие.
 *
 * @param visible показывать ли баннер (нет сети)
 * @param cachedAt время последнего успешного обновления данных, мс (null — неизвестно)
 */
@Composable
fun OfflineBanner(
    visible: Boolean,
    cachedAt: Long? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(NexoryColors.Error.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CloudOff, null, tint = NexoryColors.Error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Нет подключения к интернету",
                    color = NexoryColors.Error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append("Показаны сохранённые данные")
                        cachedAt?.let { append(" · ").append(relativeTime(it)) }
                    },
                    color = NexoryColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/** «только что» / «5 мин назад» / «2 ч назад» / «12 июн». */
private fun relativeTime(timestampMs: Long): String {
    val diffMin = (System.currentTimeMillis() - timestampMs) / 60_000
    return when {
        diffMin < 1   -> "обновлено только что"
        diffMin < 60  -> "обновлено $diffMin мин назад"
        diffMin < 1440 -> "обновлено ${diffMin / 60} ч назад"
        else -> {
            val days = diffMin / 1440
            "обновлено $days ${plural(days, "день", "дня", "дней")} назад"
        }
    }
}

private fun plural(n: Long, one: String, few: String, many: String): String {
    val mod10 = n % 10; val mod100 = n % 100
    return when {
        mod10 == 1L && mod100 != 11L -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}
