package com.nexory.app.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Результат загрузки изображения. */
sealed interface UploadResult {
    data class Success(val url: String) : UploadResult
    data class Failure(val message: String) : UploadResult
}

/**
 * Загружает выбранное изображение на сервер и возвращает постоянный URL.
 *
 * ПОЧЕМУ ОБЯЗАТЕЛЬНО СЖАТИЕ НА КЛИЕНТЕ:
 * раньше файл читался целиком (`readBytes()`) и уходил на сервер в оригинале.
 * Фотография с современной камеры весит 5–15 МБ, а на пути стоят два лимита:
 *   - multer на бэкенде: 8 МБ → ответ 400;
 *   - nginx `client_max_body_size`: по умолчанию всего 1 МБ → обрыв с 413.
 * В результате загрузка обложки мероприятия почти всегда падала, а ошибка
 * проглатывалась в `null` — пользователь видел спиннер и пустоту.
 *
 * Теперь изображение уменьшается до [MAX_DIMENSION] и пережимается в JPEG на устройстве:
 * типичный размер после этого — 200–600 КБ, что проходит любые лимиты и экономит
 * трафик пользователю. Ориентация из EXIF применяется вручную, т.к. при
 * переупаковке в новый JPEG исходные EXIF-теги теряются.
 */
@Singleton
class MediaUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NexoryApi,
) {
    private companion object {
        /** Длинная сторона после сжатия. Сервер дополнительно ужимает до 1080. */
        const val MAX_DIMENSION = 1920
        const val JPEG_QUALITY = 85
        /** Страховка: если после сжатия всё ещё великовато — не тратим трафик впустую. */
        const val MAX_UPLOAD_BYTES = 6 * 1024 * 1024
    }

    /** Совместимый со старым кодом вариант: null при любой ошибке. */
    suspend fun upload(uri: Uri): String? =
        (uploadWithResult(uri) as? UploadResult.Success)?.url

    /** Основной вариант: возвращает конкретную причину сбоя для показа пользователю. */
    suspend fun uploadWithResult(uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        val bytes = try {
            compressImage(uri)
        } catch (e: OutOfMemoryError) {
            return@withContext UploadResult.Failure(
                "Фото слишком большое для обработки. Попробуйте выбрать снимок меньшего размера"
            )
        } catch (e: Exception) {
            return@withContext UploadResult.Failure(
                "Не удалось прочитать фото. Попробуйте выбрать другое изображение"
            )
        } ?: return@withContext UploadResult.Failure(
            "Не удалось прочитать изображение. Попробуйте выбрать другое фото"
        )

        if (bytes.size > MAX_UPLOAD_BYTES) {
            return@withContext UploadResult.Failure(
                "Фото слишком большое даже после сжатия. Выберите другое изображение"
            )
        }

        try {
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "upload.jpg", body)
            val url = api.uploadImage(part)["url"]
            if (url.isNullOrBlank()) {
                UploadResult.Failure("Сервер не вернул ссылку на файл. Попробуйте ещё раз")
            } else {
                UploadResult.Success(url)
            }
        } catch (e: Exception) {
            // Конкретизируем: пользователю важно понимать, чинить сеть или менять фото
            val parsed = ApiError.parse(e)
            val message = when (parsed.code) {
                413  -> "Сервер отклонил файл: слишком большой размер"
                415  -> "Неподдерживаемый формат изображения"
                401  -> "Сессия истекла. Войдите заново и повторите загрузку"
                else -> parsed.message
            }
            UploadResult.Failure(message)
        }
    }

    /**
     * Уменьшает и пережимает изображение. Двухпроходное декодирование:
     * сначала только размеры (inJustDecodeBounds), затем — с inSampleSize,
     * чтобы не поднимать в память полноразмерный битмап на 12 Мп.
     */
    private fun compressImage(uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        // Проход 1: узнаём размеры без выделения памяти под пиксели.
        //
        // ВНИМАНИЕ: при inJustDecodeBounds = true метод decodeStream ВСЕГДА возвращает
        // null — это его контракт, размеры он кладёт в options.outWidth/outHeight.
        // Поэтому проверять на null нужно результат openInputStream, а НЕ результат
        // декодирования: иначе функция обрывается на любом, даже корректном файле.
        val boundsStream = resolver.openInputStream(uri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Проход 2: декодируем сразу уменьшенным.
        // Здесь inJustDecodeBounds выключен, поэтому null означает реальную ошибку
        // декодирования (битый или неподдерживаемый файл).
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val pixelStream = resolver.openInputStream(uri) ?: return null
        var bitmap = pixelStream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        // Точное уменьшение до целевого размера (inSampleSize даёт только степени двойки)
        bitmap = scaleDown(bitmap)

        // Применяем поворот из EXIF — иначе фото «ляжет на бок» после переупаковки
        bitmap = applyExifRotation(uri, bitmap)

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    /**
     * Во сколько раз уменьшать при декодировании (только степени двойки).
     *
     * Считаем по ДЛИННОЙ стороне, потому что именно её мы ограничиваем в [scaleDown].
     * Прежнее условие «обе стороны больше порога» было слишком осторожным: для снимка
     * 4000×3000 оно давало sample = 1, и в память поднимался битмап на 48 МБ
     * (12 Мп × 4 байта) — на слабых устройствах это OutOfMemory ещё до сжатия.
     * По длинной стороне получается sample = 2 и ~12 МБ при том же итоговом качестве.
     */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sample = 1
        var maxSide = maxOf(width, height)
        while (maxSide / 2 >= MAX_DIMENSION) {
            maxSide /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(source: Bitmap): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= MAX_DIMENSION) return source
        val ratio = MAX_DIMENSION.toFloat() / maxSide
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== source) source.recycle()
        return scaled
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) { 0f }

        if (degrees == 0f) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (_: OutOfMemoryError) {
            bitmap // не смогли повернуть — лучше отдать как есть, чем упасть
        }
    }
}
