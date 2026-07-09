package com.nexory.app.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Ранее здесь был uCrop (внешний экран кадрирования), но он приводил к вылету
// приложения на некоторых устройствах. Сейчас — безопасный проброс: выбранное
// изображение сразу уходит на загрузку, а сервер сам ужимает его до 1080px.
// API сохранён (rememberImageCropper), чтобы места вызова не менять.
@Composable
fun rememberImageCropper(
    aspectX: Float = 1f,
    aspectY: Float = 1f,
    circle: Boolean = true,
    onResult: (Uri) -> Unit,
): (Uri) -> Unit {
    return remember { { source: Uri -> onResult(source) } }
}
