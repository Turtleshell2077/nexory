package com.nexory.app.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Прокручивает экран так, чтобы поле ввода было полностью видно над клавиатурой.
 *
 * Зачем отдельный модификатор: раньше это поведение приходилось повторять вручную
 * на каждом экране, из-за чего на части полей оно просто терялось, а на новых
 * экранах о нём забывали. Теперь достаточно навесить `Modifier.scrollOnFocus()`
 * на текстовое поле — и поведение появляется само.
 *
 * Как работает: при получении фокуса поле просит родительский скролл-контейнер
 * показать себя целиком (BringIntoViewRequester). Небольшая задержка нужна, чтобы
 * запрос ушёл ПОСЛЕ того, как клавиатура выехала и `imePadding` пересчитал отступ —
 * иначе прокрутка происходит по старым размерам и поле всё равно остаётся закрытым.
 *
 * Требование: контейнер должен быть прокручиваемым (verticalScroll / LazyColumn)
 * и иметь [Modifier.imePadding] — см. [imeAwareScroll].
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.scrollOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    // Помним предыдущее состояние фокуса: прокручивать нужно только в момент,
    // когда поле его ПОЛУЧАЕТ по действию пользователя.
    //
    // Без этой проверки onFocusEvent срабатывал и при обычной рекомпозиции —
    // например, после смены аватара, когда поля пересоздавались по ключу
    // remember(state.user). Уже сфокусированное поле повторно просило показать
    // себя, и экран самопроизвольно прыгал вверх.
    var wasFocused by remember { mutableStateOf(false) }

    this
        .bringIntoViewRequester(requester)
        .onFocusEvent { state ->
            val gainedFocus = state.isFocused && !wasFocused
            wasFocused = state.isFocused
            if (gainedFocus) {
                scope.launch {
                    // Просим показать себя дважды.
                    //
                    // Первый запрос — быстрый отклик, пока клавиатура выезжает.
                    // Но к этому моменту imePadding ещё не пересчитал отступ, и
                    // прокрутка идёт по старым размерам: на невысоких экранах и
                    // внутри модальных шторок поле всё равно оставалось закрытым.
                    // Второй запрос приходится на момент, когда клавиатура уже
                    // на месте и размеры окончательные.
                    delay(120)
                    runCatching { requester.bringIntoView() }
                    delay(320)
                    runCatching { requester.bringIntoView() }
                }
            }
        }
}

/**
 * То же самое, но для ЦЕЛОГО блока, а не одного поля.
 *
 * Нужно там, где поле ввода бессмысленно смотреть в отрыве от соседей: у фильтра
 * цены рядом с полем стоит шкала, и подводить к клавиатуре одно только поле —
 * значит спрятать шкалу, ради которой пользователь сюда и пришёл.
 *
 * [active] — момент, когда блок нужно показать (например, открылся ввод суммы).
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun rememberBringIntoView(active: Boolean): BringIntoViewRequester {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(active) {
        if (active) {
            delay(120)
            runCatching { requester.bringIntoView() }
            delay(320)
            runCatching { requester.bringIntoView() }
        }
    }
    return requester
}

/**
 * Отступ под клавиатуру для прокручиваемого контейнера с полями ввода.
 * Вынесено отдельным именем, чтобы на новых экранах было видно намерение
 * и не приходилось вспоминать про imePadding.
 */
fun Modifier.imeAwareScroll(): Modifier = this.imePadding()
