package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.MoscowMetro
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors

// -------------------------------------------------------
// Переиспользуемые цвета для текстовых полей
// -------------------------------------------------------
@Composable
fun nexoryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = NexoryColors.PrimaryBlue,
    unfocusedBorderColor    = NexoryColors.SurfaceMid,
    focusedContainerColor   = NexoryColors.SurfaceMid,
    unfocusedContainerColor = NexoryColors.SurfaceMid,
    cursorColor             = NexoryColors.PrimaryBlue,
    focusedTextColor        = NexoryColors.TextPrimary,
    unfocusedTextColor      = NexoryColors.TextPrimary,
    focusedLabelColor       = NexoryColors.PrimaryBlue,
    unfocusedLabelColor     = NexoryColors.TextSecondary,
    focusedLeadingIconColor   = NexoryColors.PrimaryBlue,
    unfocusedLeadingIconColor = NexoryColors.TextSecondary,
)

// -------------------------------------------------------
// Стилизованное текстовое поле
// -------------------------------------------------------
@Composable
fun NexoryField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    icon:          ImageVector,
    maxLines:      Int          = 1,
    keyboardType:  KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        // scrollOnFocus — поле само поднимается над клавиатурой при фокусе
        modifier        = Modifier.fillMaxWidth().scrollOnFocus(),
        label           = { Text(label) },
        leadingIcon     = { Icon(icon, null) },
        maxLines        = maxLines,
        singleLine      = maxLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape           = RoundedCornerShape(12.dp),
        colors          = nexoryTextFieldColors(),
    )
}

// -------------------------------------------------------
// Пустое состояние (нет друзей, нет чатов и т.д.)
// -------------------------------------------------------
@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                null,
                tint     = NexoryColors.TextSecondary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(text, color = NexoryColors.TextSecondary, fontSize = 16.sp)
        }
    }
}

// -------------------------------------------------------
// Аватар пользователя. Если фото нет — рисуем яркий «генеративный»
// аватар: уникальный градиент по имени + первая буква. Одинаковый
// человек всегда получает один и тот же цвет — узнаваемо и не скучно.
// -------------------------------------------------------
// Палитра вынесена в AvatarPresets — её же показывает экран выбора аватара.

/**
 * Аватар пользователя. Три режима, в порядке приоритета:
 *  1. обычная ссылка на файл — показываем фотографию;
 *  2. строка вида `preset:N` — пользователь сам выбрал вариант оформления;
 *  3. пусто — детерминированный градиент по [seed] (у каждого свой цвет).
 */
@Composable
fun UserAvatar(
    url:      String?,
    name:     String?,
    seed:     String? = null,   // стабильный ключ (обычно id пользователя)
    size:     Dp,
    modifier: Modifier = Modifier,
) {
    if (AvatarPresets.isRealPhoto(url)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape).background(NexoryColors.SurfaceMid),
        )
    } else {
        // Фото нет (или в БД осталась строка от прежней версии с шаблонами) —
        // рисуем градиент. Палитра и инициалы детерминированы, поэтому считаем
        // их один раз на ключ, а не на каждую рекомпозицию: аватары рисуются
        // в длинных списках друзей и чатов.
        val key = (seed ?: name ?: "?")
        val palette = remember(key) { AvatarPresets.paletteForKey(key) }
        val initials = remember(name) { AvatarPresets.initialsOf(name) }
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}


// -------------------------------------------------------
// Поле выбора станции метро с автоподбором (метро Москвы).
// Вся механика автоподбора живёт в общем AutocompleteTextField —
// здесь остаётся только источник подсказок.
// -------------------------------------------------------
@Composable
fun MetroAutocompleteField(
    value:    String,
    onChange: (String) -> Unit,
    label:    String = "Начните вводить станцию",
) {
    // remember обязателен: без него фильтрация ~300 станций выполнялась бы
    // на каждой рекомпозиции, а не только при изменении текста
    val suggestions = remember(value) { MoscowMetro.suggest(value) }

    AutocompleteTextField(
        value = value,
        onValueChange = onChange,
        suggestions = suggestions,
        onSuggestionPick = onChange,
        placeholder = label,
        leadingIcon = Icons.Default.Place,
    )
}


// -------------------------------------------------------
// Метка секции в формах
// -------------------------------------------------------
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = NexoryColors.TextSecondary,
        modifier   = Modifier.padding(top = 4.dp),
    )
}