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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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