package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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