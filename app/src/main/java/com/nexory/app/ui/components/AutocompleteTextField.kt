package com.nexory.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.nexory.app.ui.theme.NexoryColors

/**
 * Единый компонент поля ввода с автоподбором.
 *
 * Раньше автоподбор был написан отдельно в каждом месте (метро, увлечения в
 * профиле, увлечения в фильтре), реализации разъехались, и в части из них
 * список подсказок был обычным элементом разметки — он занимал место, толкал
 * поле вниз под клавиатуру, а само поле с набранным текстом пропадало с экрана.
 *
 * Здесь это решено раз и навсегда:
 *  - список рисуется во всплывающем окне [Popup] и на разметку НЕ влияет,
 *    поэтому при его появлении и скрытии ничего не смещается;
 *  - [Popup] позиционируется НАД полем — не перекрывает ни поле, ни клавиатуру;
 *  - высота списка ограничена, длинные результаты прокручиваются;
 *  - поле помечено [scrollOnFocus], поэтому при фокусе экран сам подводит его
 *    над клавиатурой.
 *
 * @param suggestions     подсказки; пустой список — окно не показывается
 * @param onSuggestionPick выбор подсказки из списка
 * @param allowCustomValue разрешить добавить значение, которого нет в списке
 * @param onCustomValueAdd вызывается при добавлении своего значения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    allowCustomValue: Boolean = false,
    onCustomValueAdd: (String) -> Unit = {},
    isError: Boolean = false,
    maxSuggestionsHeight: androidx.compose.ui.unit.Dp = 200.dp,
) {
    // Подсказки прячем сразу после выбора, иначе окно «залипает» над полем
    var suppressed by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    // После выбора значение считается введённым: снимаем фокус, клавиатура
    // убирается и не закрывает собой только что добавленный элемент. Чтобы
    // добавить следующий, пользователь снова нажимает на поле — это осознанное
    // действие, а не случайный ввод в поле, о котором он уже забыл.
    fun commit(action: () -> Unit) {
        suppressed = true
        focusManager.clearFocus()
        action()
    }

    val trimmed = value.trim()
    val canAddCustom = allowCustomValue && trimmed.isNotEmpty() &&
        suggestions.none { it.equals(trimmed, ignoreCase = true) }
    val visible = !suppressed && (suggestions.isNotEmpty() || canAddCustom)

    // Позиционер поднимает окно ровно на его высоту над полем.
    // Если сверху не хватает места — прижимаем к верху экрана, а не за него.
    val abovePosition = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = 8
                val y = (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
                return IntOffset(anchorBounds.left, y)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { suppressed = false; onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidthPx = it.width }
                .scrollOnFocus(),
            placeholder = { Text(placeholder, color = NexoryColors.TextSecondary) },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = NexoryColors.TextSecondary) } },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { suppressed = true; onValueChange("") }) {
                        Icon(Icons.Default.Cancel, "Очистить", tint = NexoryColors.TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = nexoryTextFieldColors(),
        )

        if (visible && fieldWidthPx > 0) {
            Popup(
                popupPositionProvider = abovePosition,
                // focusable = false — клавиатура не должна закрываться при показе списка
                properties = PopupProperties(focusable = false),
                onDismissRequest = { suppressed = true },
            ) {
                Column(
                    modifier = Modifier
                        .width(with(density) { fieldWidthPx.toDp() })
                        .heightIn(max = maxSuggestionsHeight)
                        .background(NexoryColors.SurfaceMid, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState()),
                ) {
                    suggestions.forEach { item ->
                        Text(
                            item,
                            color = NexoryColors.TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { commit { onSuggestionPick(item) } }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    if (canAddCustom) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { commit { onCustomValueAdd(trimmed) } }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Add, null, tint = NexoryColors.PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Добавить «$trimmed»",
                                color = NexoryColors.PrimaryBlue,
                                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
