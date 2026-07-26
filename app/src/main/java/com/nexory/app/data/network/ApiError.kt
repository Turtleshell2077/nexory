package com.nexory.app.data.network

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

// Тело ошибки, которое отдаёт бэкенд: { error, details: [{ field, msg }] }
data class FieldErrorDto(val field: String? = null, val msg: String? = null)
data class ApiErrorBody(val error: String? = null, val details: List<FieldErrorDto>? = null)

// Разобранная ошибка для UI: понятное сообщение + список невалидных полей.
data class ParsedError(
    val message: String,
    val fields:  List<String> = emptyList(),
    val code:    Int?         = null,
)

/**
 * Превращает любое исключение сети/HTTP в понятное пользователю сообщение.
 * Приоритет — текст ошибки от сервера (там уже русские сообщения валидации),
 * иначе — дефолт по HTTP-коду. Никаких «HTTP 400» для пользователя.
 */
object ApiError {
    private val gson = Gson()

    fun parse(e: Throwable): ParsedError = when (e) {
        is HttpException -> {
            val code = e.code()
            val raw = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val body = raw?.takeIf { it.isNotBlank() }?.let {
                try { gson.fromJson(it, ApiErrorBody::class.java) } catch (_: Exception) { null }
            }
            val serverMsg = body?.error?.takeIf { it.isNotBlank() }
            // Если сервер прислал детали по полям — добавим первую в сообщение
            val detailMsg = body?.details?.firstOrNull()?.msg?.takeIf { it.isNotBlank() }
            ParsedError(
                message = serverMsg ?: detailMsg ?: defaultForCode(code),
                fields  = body?.details?.mapNotNull { it.field }?.distinct() ?: emptyList(),
                code    = code,
            )
        }
        is IOException -> ParsedError("Нет связи с сервером. Проверьте интернет-соединение")
        else -> ParsedError(e.message ?: "Что-то пошло не так. Попробуйте ещё раз")
    }

    // Короткий помощник, когда нужен только текст
    fun message(e: Throwable): String = parse(e).message

    private fun defaultForCode(code: Int): String = when (code) {
        400 -> "Проверьте правильность заполнения полей"
        401 -> "Требуется вход в аккаунт"
        403 -> "Недостаточно прав для этого действия"
        404 -> "Не найдено"
        409 -> "Такие данные уже существуют"
        413 -> "Файл слишком большой"
        429 -> "Слишком много попыток. Подождите немного"
        in 500..599 -> "Ошибка на сервере. Мы уже разбираемся — попробуйте позже"
        else -> "Не удалось выполнить запрос (код $code)"
    }
}
