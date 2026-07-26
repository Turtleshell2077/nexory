package com.nexory.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Единый безопасный способ открыть внешнюю ссылку (документы, платёжная страница).
 *
 * Обрабатывает оба реальных сбоя:
 *  - на устройстве нет браузера / нет приложения, способного открыть ссылку
 *    (ActivityNotFoundException) — частый кейс на «чистых» прошивках и эмуляторах;
 *  - любое иное исключение при старте Activity.
 *
 * @return true, если внешнее приложение действительно было запущено.
 */
fun openExternalUrl(
    context: Context,
    url: String,
    errorMessage: String = "Не удалось открыть ссылку. Проверьте, что установлен браузер",
): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        false
    } catch (e: Exception) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        false
    }
}
