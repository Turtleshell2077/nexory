package com.nexory.app.ui.screens.events

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale("ru")
private val MONTHS_SHORT = listOf(
    "янв", "фев", "мар", "апр", "мая", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек"
)

// Парсит ISO-строку из бэкенда в локальное время устройства. null при ошибке.
private fun parse(iso: String?): OffsetDateTime? {
    if (iso.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
    } catch (_: Exception) {
        try {
            // Фоллбэк: "2026-06-12T18:00:00" без зоны
            OffsetDateTime.parse(iso + "Z")
        } catch (_: Exception) { null }
    }
}

private fun timeStr(dt: OffsetDateTime): String =
    "%02d:%02d".format(dt.hour, dt.minute)

private fun dateStr(dt: OffsetDateTime): String =
    "${dt.dayOfMonth} ${MONTHS_SHORT[dt.monthValue - 1]}"

// "12 июн · 18:00–20:00", "12 июн · 18:00" без окончания,
// "12 июн 22:00 – 13 июн 02:00" если мероприятие переходит на следующий день
fun formatEventDateTime(startsAt: String?, endsAt: String? = null): String {
    val start = parse(startsAt) ?: return startsAt?.take(10) ?: ""
    val end   = parse(endsAt)?.let { if (it.isBefore(start)) it.plusDays(1) else it }
        ?: return "${dateStr(start)} · ${timeStr(start)}"
    // Ночное мероприятие заканчивается уже другого числа — молча показывать
    // «22:00–02:00» без даты значит вводить в заблуждение
    return if (start.toLocalDate() == end.toLocalDate())
        "${dateStr(start)} · ${timeStr(start)}–${timeStr(end)}"
    else
        "${dateStr(start)} ${timeStr(start)} – ${dateStr(end)} ${timeStr(end)}"
}

// "12 июн" — короткая дата для компактных мест
fun formatEventDateShort(startsAt: String?): String {
    val start = parse(startsAt) ?: return startsAt?.take(10) ?: ""
    return dateStr(start)
}

// "18:00–20:00" — только время
fun formatEventTimeRange(startsAt: String?, endsAt: String?): String {
    val start = parse(startsAt) ?: return ""
    val end   = parse(endsAt)?.let { if (it.isBefore(start)) it.plusDays(1) else it }
    return if (end != null) "${timeStr(start)}–${timeStr(end)}" else timeStr(start)
}

// "Создано сегодня" / "Создано вчера" / "Создано 3 дня назад" / "Создано 12 июн 2026"
fun formatCreatedAt(createdAt: String?): String {
    val dt = parse(createdAt) ?: return ""
    val now = OffsetDateTime.now()
    val days = java.time.Duration.between(dt, now).toDays()
    return when {
        days <= 0L && dt.dayOfYear == now.dayOfYear -> "Создано сегодня"
        days <= 1L -> "Создано вчера"
        days < 7L  -> "Создано ${days} ${plural(days, "день", "дня", "дней")} назад"
        else       -> "Создано ${dt.dayOfMonth} ${MONTHS_SHORT[dt.monthValue - 1]} ${dt.year}"
    }
}

// Русское склонение: 1 день, 2 дня, 5 дней
private fun plural(n: Long, one: String, few: String, many: String): String {
    val mod10 = n % 10; val mod100 = n % 100
    return when {
        mod10 == 1L && mod100 != 11L -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

/**
 * Момент, когда мероприятие действительно заканчивается.
 *
 * Тонкость: пикер времени долго собирал дату окончания из ТОЙ ЖЕ календарной
 * даты, что и начало. Для мероприятия «сегодня 22:00 – завтра 02:00» это давало
 * ends_at = сегодня 02:00, то есть РАНЬШЕ начала и уже в прошлом. Такие записи
 * лежат в базе до сих пор, поэтому здесь мы их восстанавливаем: окончание
 * раньше начала может значить только переход через полночь — добавляем сутки.
 *
 * Сам пикер тоже исправлен (см. DateTimeWheelDialog), так что новые записи
 * приходят корректными.
 */
private fun finishMoment(startsAt: String?, endsAt: String?): OffsetDateTime? {
    val start = parse(startsAt)
    val end   = parse(endsAt)
    if (end == null) return start
    if (start == null) return end
    return if (end.isBefore(start)) end.plusDays(1) else end
}

/**
 * Мероприятие уже прошло?
 *
 * Ориентируемся на время ОКОНЧАНИЯ: пока мероприятие идёт, называть его
 * завершённым неверно — человек ещё может присоединиться.
 */
fun isEventFinished(startsAt: String?, endsAt: String? = null): Boolean {
    val finish = finishMoment(startsAt, endsAt) ?: return false
    return finish.toInstant().isBefore(java.time.Instant.now())
}

// "Бесплатно" или "500 ₽"
fun formatPrice(price: Double?): String {
    if (price == null || price <= 0.0) return "Бесплатно"
    val rounded = if (price % 1.0 == 0.0) price.toInt().toString() else price.toString()
    return "$rounded ₽"
}
