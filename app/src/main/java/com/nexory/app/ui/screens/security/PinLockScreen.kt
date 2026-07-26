package com.nexory.app.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexory.app.ui.theme.NexoryColors
import kotlinx.coroutines.delay

/**
 * Экран блокировки по PIN. Универсальный: используется и для ВВОДА PIN при входе,
 * и для УСТАНОВКИ нового PIN (с подтверждением).
 *
 * @param mode          [PinMode.ENTER] — проверить, [PinMode.SET] — задать новый.
 * @param onCheck       для ENTER: проверка введённого кода (true = верный).
 * @param onPinSet      для SET: сюда приходит установленный код (после подтверждения).
 * @param onSuccess     вызывается при успехе (ENTER — верный код).
 * @param onCancel      закрыть экран (null → кнопки отмены нет, напр. при входе).
 */
enum class PinMode { ENTER, SET }

private const val PIN_LENGTH = 4

@Composable
fun PinLockScreen(
    mode: PinMode,
    onCheck: (suspend (String) -> Boolean)? = null,
    onPinSet: ((String) -> Unit)? = null,
    onSuccess: () -> Unit = {},
    onCancel: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) } // для режима SET (первый ввод)
    var error by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    val title = when {
        mode == PinMode.SET && firstPin == null -> "Придумайте PIN-код"
        mode == PinMode.SET -> "Повторите PIN-код"
        else -> "Введите PIN-код"
    }

    // Обработка заполнения PIN
    suspend fun onFilled(code: String) {
        when (mode) {
            PinMode.SET -> {
                if (firstPin == null) {
                    firstPin = code
                    pin = ""
                } else if (firstPin == code) {
                    onPinSet?.invoke(code)
                    onSuccess()
                } else {
                    error = true
                    firstPin = null
                    pin = ""
                }
            }
            PinMode.ENTER -> {
                checking = true
                val ok = onCheck?.invoke(code) ?: false
                checking = false
                if (ok) onSuccess()
                else { error = true; pin = "" }
            }
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            onFilled(pin)
        }
    }
    // Сброс тряски-ошибки
    LaunchedEffect(error) {
        if (error) { delay(600); error = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoryColors.DeepBlack)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onCancel != null) {
            Box(Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Отмена", color = NexoryColors.TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        Icon(Icons.Default.Lock, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(20.dp))
        Text(title, color = NexoryColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            if (error) "Неверный PIN-код, попробуйте ещё раз" else "Код нужен для входа в приложение",
            color = if (error) NexoryColors.Error else NexoryColors.TextSecondary,
            fontSize = 13.sp, textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        // Точки-индикаторы
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PIN_LENGTH) { i ->
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                error -> NexoryColors.Error
                                filled -> NexoryColors.PrimaryBlue
                                else -> NexoryColors.SurfaceMid
                            }
                        ),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Клавиатура
        val enabled = !checking
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { d -> PinKey(d, enabled) { if (pin.length < PIN_LENGTH) pin += d } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(72.dp))
                PinKey("0", enabled) { if (pin.length < PIN_LENGTH) pin += "0" }
                // Удалить
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled && pin.isNotEmpty()) { pin = pin.dropLast(1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Backspace, "Удалить", tint = NexoryColors.TextSecondary, modifier = Modifier.size(26.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PinKey(digit: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(NexoryColors.SurfaceDark)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(digit, color = NexoryColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}
