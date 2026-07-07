package com.nexory.app.data.network

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

// Загружает выбранное из галереи изображение (content:// Uri) на сервер
// и возвращает постоянный http-URL, который можно сохранить как avatar_url/cover_url.
@Singleton
class MediaUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NexoryApi,
) {
    suspend fun upload(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext  = when {
                mime.contains("png")  -> "png"
                mime.contains("webp") -> "webp"
                else                  -> "jpg"
            }
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "upload.$ext", body)
            api.uploadImage(part)["url"]
        } catch (e: Exception) {
            null
        }
    }
}
