package com.nexory.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Состояние и запрос разрешения на показ уведомлений.
 *
 * Зачем отдельный компонент: разрешение POST_NOTIFICATIONS было объявлено в манифесте,
 * но НИКОГДА не запрашивалось в рантайме. На Android 13+ (API 33) это означает, что
 * push-уведомления молча не приходят — пользователь считает, что функция сломана.
 *
 * Требование модерации: разрешение запрашивается непосредственно перед использованием
 * и с понятным объяснением. Поэтому запрос вызывается из Настроек в момент, когда
 * пользователь включает уведомления, а не «пачкой» при первом запуске.
 *
 * На Android 12 и ниже разрешение не требуется — [granted] сразу true.
 */
class NotificationPermissionState(
    val granted: Boolean,
    val shouldAsk: Boolean,
    private val onRequest: () -> Unit,
) {
    /** Запросить разрешение системным диалогом. */
    fun request() = onRequest()
}

@Composable
fun rememberNotificationPermission(): NotificationPermissionState {
    val context = LocalContext.current
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var granted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted -> granted = isGranted }

    // Перепроверяем при возврате в приложение: пользователь мог выдать разрешение
    // вручную в системных настройках.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                granted = isNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return NotificationPermissionState(
        granted = granted,
        shouldAsk = needsPermission && !granted,
        onRequest = {
            if (needsPermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
    )
}

private fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

/** Открыть системные настройки уведомлений приложения (если пользователь отказал навсегда). */
fun openAppNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { /* настройки недоступны — молча игнорируем */ }
}
