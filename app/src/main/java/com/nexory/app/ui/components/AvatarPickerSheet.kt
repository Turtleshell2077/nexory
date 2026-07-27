package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexory.app.ui.theme.NexoryColors

/**
 * Действия с фотографией профиля: загрузить из галереи или удалить.
 *
 * Выбора оформления здесь намеренно нет: если фотографии нет, пользователь
 * автоматически получает градиентный аватар, подобранный по его id. Прежний
 * экран с шаблонами убран — он усложнял интерфейс без пользы.
 *
 * Все действия собраны в этом листе: раньше кнопка удаления висела прямо
 * на экране профиля и загромождала его.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    currentUrl: String?,
    userName: String?,
    onPickPhoto: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasPhoto = AvatarPresets.isRealPhoto(currentUrl)
    var confirmRemove by remember { mutableStateOf(false) }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = NexoryColors.Error, modifier = Modifier.size(32.dp)) },
            title = { Text("Удалить фото?", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Фотография будет удалена. Вместо неё другие увидят цветной " +
                        "аватар с вашими инициалами. Загрузить новое фото можно в любой момент.",
                    color = NexoryColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove(); onDismiss() }) {
                    Text("Удалить", color = NexoryColors.Error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("Отмена", color = NexoryColors.TextSecondary)
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NexoryColors.SurfaceDark,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("Фото профиля", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)
            Spacer(Modifier.height(16.dp))

            ActionRow(
                icon = Icons.Default.PhotoLibrary,
                title = "Выбрать из галереи",
                subtitle = "Загрузить свою фотографию",
                onClick = { onPickPhoto(); onDismiss() },
            )

            if (hasPhoto) {
                Spacer(Modifier.height(8.dp))
                ActionRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "Удалить фото",
                    subtitle = "Вернуться к аватару с инициалами",
                    tint = NexoryColors.Error,
                    onClick = { confirmRemove = true },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = NexoryColors.PrimaryBlue,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NexoryColors.SurfaceMid)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (tint == NexoryColors.Error) tint else NexoryColors.TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.Medium,
            )
            Text(subtitle, color = NexoryColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
