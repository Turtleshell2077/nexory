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
 * Выбор аватара: фотография из галереи, сгенерированный аватар или удаление фото.
 *
 * Механика генерируемых аватаров: шесть стилей (градиент и пять узорных).
 * Стиль нажимается — и раскрывается палитра цветовых вариантов именно этого стиля.
 * Так пользователь сначала выбирает «характер» аватара, а потом цвет, и ему не
 * приходится листать десятки одинаковых кружков.
 *
 * Все действия с аватаром собраны здесь: раньше кнопка удаления висела прямо
 * на экране профиля и загромождала его.
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
) {
    val hasPhoto = !currentUrl.isNullOrBlank() && !AvatarPresets.isPreset(currentUrl)
    val current = AvatarPresets.parse(currentUrl)
    val initials = remember(userName) { AvatarPresets.initialsOf(userName) }

    // Раскрытый стиль. По умолчанию раскрываем тот, что уже выбран.
    var expandedStyle by remember { mutableStateOf(current?.style) }
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

            Spacer(Modifier.height(22.dp))
            Text("Или соберите аватар", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(
                "Нажмите стиль, чтобы выбрать цвет",
                fontSize = 12.sp, color = NexoryColors.TextSecondary,
            )
            Spacer(Modifier.height(14.dp))

            AvatarPresets.Style.entries.forEach { style ->
                val isExpanded = expandedStyle == style
                val isCurrentStyle = current?.style == style
                // В свёрнутом виде показываем стиль в цвете, который у него выбран
                val previewVariant = if (isCurrentStyle) current!!.variant else 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCurrentStyle) NexoryColors.PrimaryBlue.copy(alpha = 0.10f)
                            else NexoryColors.SurfaceMid
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStyle = if (isExpanded) null else style }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GeneratedAvatar(
                            selection = AvatarPresets.Selection(style, previewVariant),
                            initials = initials,
                            size = 44.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(style.title, color = NexoryColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (isCurrentStyle) {
                                Text("Выбран", color = NexoryColors.PrimaryBlue, fontSize = 12.sp)
                            }
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Свернуть" else "Показать цвета",
                            tint = NexoryColors.TextSecondary,
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        // Горизонтальная лента цветов этого стиля
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            repeat(AvatarPresets.variantCount) { variant ->
                                val selected = isCurrentStyle && current?.variant == variant
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(
                                            if (selected) Modifier.border(3.dp, NexoryColors.PrimaryBlue, CircleShape)
                                            else Modifier
                                        )
                                        .clickable {
                                            onPickPreset(AvatarPresets.toUrl(style, variant))
                                            onDismiss()
                                        },
                                ) {
                                    GeneratedAvatar(
                                        selection = AvatarPresets.Selection(style, variant),
                                        initials = initials,
                                        size = 52.dp,
                                    )
                                }
                            }
                        }
                    }
                }
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
