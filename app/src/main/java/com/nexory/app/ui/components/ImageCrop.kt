package com.nexory.app.ui.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yalantis.ucrop.UCrop
import java.io.File

// Возвращает функцию launchCrop(sourceUri): открывает экран кадрирования uCrop
// и вызывает onResult с обрезанным изображением. Пользователь сам выбирает кадр (pan/zoom).
@Composable
fun rememberImageCropper(
    aspectX: Float = 1f,
    aspectY: Float = 1f,
    circle: Boolean = true,
    onResult: (Uri) -> Unit,
): (Uri) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            UCrop.getOutput(result.data!!)?.let(onResult)
        }
    }
    return remember(aspectX, aspectY, circle) {
        { source: Uri ->
            val dest = Uri.fromFile(File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))
            val options = UCrop.Options().apply {
                setCircleDimmedLayer(circle)
                setShowCropGrid(!circle)
                setShowCropFrame(!circle)
                setCompressionQuality(85)
                setToolbarTitle("Кадрирование")
                setFreeStyleCropEnabled(false)
            }
            val intent = UCrop.of(source, dest)
                .withAspectRatio(aspectX, aspectY)
                .withMaxResultSize(1080, 1080)
                .withOptions(options)
                .getIntent(context)
            launcher.launch(intent)
        }
    }
}
