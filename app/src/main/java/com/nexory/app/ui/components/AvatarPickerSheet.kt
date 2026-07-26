package com.nexory.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * Действия с аватаром: загрузить фото, удалить фото, выбрать оформление.
 *
 * Сам выбор оформления живёт на отдельных экранах (шаблон → цвет): внутри
 * этого листа сетка вариантов получалась тесной, а перестроение содержимого
 * при выборе дёргало экран под ним.
 *
 * Все действия собраны здесь: раньше кнопка удаления висела прямо на экране
 * профиля и загромождала его.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    currentUrl: String?,
    userName: String?,
    onPickPhoto: () -> Unit,
    onPickPreset: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    /** Открыть двухшаговый выбор оформления (шаблон → цвет). */
    onOpenStylePicker: (() -> Unit)? = null,
) {
    val hasPhoto = !currentUrl.isNullOrBlank() && !AvatarPresets.isPreset(currentUrl)
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
                    "Фотография будет удалена. Вместо неё другие увидят ваш аватар " +
                        "с инициалами. Загрузить новое фото можно в любой момент.",
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
                .verticalScroll(rememberScrollState())
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

            Spacer(Modifier.height(8.dp))
            // Выбор оформления вынесен в отдельный двухшаговый флоу
            // (шаблон → цвет). Внутри листа сетка вариантов была слишком
            // тесной, а при выборе экран под ней перестраивался и прыгал.
            ActionRow(
                icon = Icons.Default.Palette,
                title = "Выбрать стиль аватара",
                subtitle = "Шаблон и цвет вместо фотографии",
                onClick = { onOpenStylePicker?.invoke(); onDismiss() },
            )
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
