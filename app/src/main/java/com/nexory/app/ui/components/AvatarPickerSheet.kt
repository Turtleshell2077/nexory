package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexory.app.ui.theme.NexoryColors

/**
 * Выбор аватара: фотография из галереи, готовый вариант оформления или «без фото».
 *
 * Раньше кнопка удаления фото висела прямо на экране профиля под аватаром и портила
 * вид. Теперь все действия с аватаром собраны в одном месте и вызываются нажатием
 * на сам аватар — это и аккуратнее, и привычнее.
 *
 * @param currentUrl  текущее значение avatar_url (ссылка, `preset:N` или пусто)
 * @param userName    имя — рисуем инициалы в превью вариантов
 * @param onPickPhoto открыть галерею
 * @param onPickPreset выбрать готовый вариант (передаётся строка вида `preset:3`)
 * @param onRemove    убрать фото и вернуться к автоматическому аватару
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
    val currentPreset = AvatarPresets.indexOf(currentUrl)
    var confirmRemove by remember { mutableStateOf(false) }

    // Подтверждение удаления настоящей фотографии.
    // Для смены варианта оформления подтверждение не нужно — это не потеря данных.
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = NexoryColors.SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = NexoryColors.Error, modifier = Modifier.size(32.dp)) },
            title = { Text("Удалить фото?", color = NexoryColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Фотография будет удалена. Вместо неё другие увидят ваш цветной " +
                        "аватар с инициалами. Загрузить новое фото можно в любой момент.",
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

            Spacer(Modifier.height(20.dp))
            Text(
                "Или выберите оформление",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexoryColors.TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Показывается вместо фото — с вашими инициалами",
                fontSize = 12.sp, color = NexoryColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))

            val initials = remember(userName) { AvatarPresets.initialsOf(userName) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(AvatarPresets.gradients.indices.toList()) { index ->
                    val selected = currentPreset == index
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(AvatarPresets.gradientAt(index)))
                            .then(
                                if (selected) Modifier.border(3.dp, NexoryColors.PrimaryBlue, CircleShape)
                                else Modifier
                            )
                            .clickable { onPickPreset(AvatarPresets.toUrl(index)); onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            Text(title, color = if (tint == NexoryColors.Error) tint else NexoryColors.TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = NexoryColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
