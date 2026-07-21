package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        modifier        = Modifier.fillMaxWidth(),
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
private val AVATAR_GRADIENTS: List<List<Color>> = listOf(
    listOf(Color(0xFF6D5DF6), Color(0xFF4A90E2)), // сине-фиолетовый
    listOf(Color(0xFFEE5A9E), Color(0xFFF7797D)), // розово-коралловый
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // изумрудный
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)), // золотой
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)), // индиго
    listOf(Color(0xFFFF6A88), Color(0xFFFF99AC)), // малиновый
    listOf(Color(0xFF00C6FB), Color(0xFF005BEA)), // голубой
    listOf(Color(0xFFF953C6), Color(0xFFB91D73)), // маджента
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)), // мятный
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)), // закатный
)

private fun gradientFor(key: String): List<Color> {
    val h = key.fold(0) { acc, c -> acc * 31 + c.code }
    val idx = ((h % AVATAR_GRADIENTS.size) + AVATAR_GRADIENTS.size) % AVATAR_GRADIENTS.size
    return AVATAR_GRADIENTS[idx]
}

private fun initialsOf(name: String?): String {
    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return "?"
    val parts = n.split(" ", "_").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        else -> n.first().uppercaseChar().toString()
    }
}

@Composable
fun UserAvatar(
    url:      String?,
    name:     String?,
    seed:     String? = null,   // стабильный ключ (обычно id пользователя)
    size:     Dp,
    modifier: Modifier = Modifier,
) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape).background(NexoryColors.SurfaceMid),
        )
    } else {
        val key = (seed ?: name ?: "?")
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientFor(key))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}

// -------------------------------------------------------
// Поле выбора станции метро с автоподбором (метро Москвы)
// -------------------------------------------------------
@Composable
fun MetroAutocompleteField(
    value:    String,
    onChange: (String) -> Unit,
    label:    String = "Начните вводить станцию",
) {
    // Показываем подсказки только когда пользователь печатает (а не после выбора)
    var showSuggestions by remember { mutableStateOf(false) }
    val suggestions = if (showSuggestions) MoscowMetro.suggest(value) else emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { showSuggestions = true; onChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(label, color = NexoryColors.TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { showSuggestions = false; onChange("") }) {
                        Icon(Icons.Default.Close, "Очистить", tint = NexoryColors.TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = nexoryTextFieldColors(),
        )
        // Список подсказок под полем
        if (suggestions.isNotEmpty() && !(suggestions.size == 1 && suggestions[0] == value)) {
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexoryColors.SurfaceMid, RoundedCornerShape(12.dp)),
            ) {
                suggestions.forEach { station ->
                    Text(
                        station,
                        color = NexoryColors.TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSuggestions = false; onChange(station) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
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