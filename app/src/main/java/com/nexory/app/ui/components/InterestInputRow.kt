package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexory.app.ui.theme.NexoryColors

/**
 * Ввод увлечения: поле с автоподбором плюс отдельная кнопка «+».
 *
 * Используется и в редактировании профиля, и в фильтре ленты — механика в обоих
 * местах должна быть одинаковой, иначе пользователь заново разбирается, как это
 * работает, на каждом экране.
 *
 * Кнопка «+» дублирует пункт «Добавить …» внутри списка подсказок. Тот пункт
 * виден только когда список раскрыт, а когда совпадений нет вовсе — списка нет
 * и добавить своё значение было неоткуда. Кнопка рядом с полем видна всегда.
 */
@Composable
fun InterestInputRow(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onPick: (String) -> Unit,
    onAddCustom: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val trimmed = query.trim()
    val canAdd = trimmed.isNotEmpty()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            AutocompleteTextField(
                value = query,
                onValueChange = onQueryChange,
                suggestions = suggestions,
                onSuggestionPick = onPick,
                placeholder = placeholder,
                allowCustomValue = true,
                onCustomValueAdd = onAddCustom,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (canAdd) NexoryColors.PrimaryBlue else NexoryColors.SurfaceMid)
                .clickable(enabled = canAdd) { onAddCustom(trimmed) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Добавить увлечение",
                tint = if (canAdd) Color.White else NexoryColors.TextSecondary,
            )
        }
    }
}
