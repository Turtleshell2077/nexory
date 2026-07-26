package com.nexory.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Готовые варианты аватара, которые пользователь может выбрать вместо фотографии.
 *
 * Как это хранится: выбранный вариант записывается в поле `avatar_url` строкой
 * вида `preset:3`. Так не потребовалась ни миграция БД, ни новое поле в API —
 * все экраны уже получают avatar_url и передают его в [UserAvatar], который сам
 * распознаёт этот формат и рисует градиент вместо загрузки картинки.
 *
 * Значения индексов менять нельзя: они уже сохранены у пользователей.
 * Новые варианты добавляйте только в конец списка.
 */
object AvatarPresets {

    /** Пары цветов для градиента. Подобраны контрастными к белому тексту инициалов. */
    val gradients: List<List<Color>> = listOf(
        listOf(Color(0xFF6D5DF6), Color(0xFF4A90E2)), // сине-фиолетовый
        listOf(Color(0xFFEE5A9E), Color(0xFFF7797D)), // розово-коралловый
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // изумрудный
        listOf(Color(0xFFF7971E), Color(0xFFFFD200)), // золотой
        listOf(Color(0xFF667EEA), Color(0xFF764BA2)), // индиго
        listOf(Color(0xFFFF6A88), Color(0xFFFF99AC)), // малиновый
        listOf(Color(0xFF00C6FB), Color(0xFF005BEA)), // голубой
        listOf(Color(0xFFF953C6), Color(0xFFB91D73)), // маджента
        listOf(Color(0xFF43E97B), Color(0xFF38F9D7)), // мятный
        listOf(Color(0xFFFA709A), Color(0xFFFEE140)), // закатный
        listOf(Color(0xFF30CFD0), Color(0xFF330867)), // морская глубина
        listOf(Color(0xFFFF8177), Color(0xFFB12A5B)), // терракотовый
    )

    /** Префикс, по которому [UserAvatar] отличает пресет от обычной ссылки на файл. */
    private const val PREFIX = "preset:"

    /** Строка для сохранения в avatar_url. */
    fun toUrl(index: Int): String = "$PREFIX$index"

    /** Это выбранный пресет, а не ссылка на картинку? */
    fun isPreset(url: String?): Boolean = url != null && url.startsWith(PREFIX)

    /**
     * Индекс пресета из строки avatar_url, либо null если это не пресет.
     * Некорректные и вышедшие за границы значения приводим к валидному индексу,
     * чтобы старое сохранённое значение не сломало отрисовку после сокращения списка.
     */
    fun indexOf(url: String?): Int? {
        if (!isPreset(url)) return null
        val raw = url!!.removePrefix(PREFIX).toIntOrNull() ?: return 0
        return raw.coerceIn(0, gradients.lastIndex)
    }

    /** Градиент для пресета. */
    fun gradientAt(index: Int): List<Color> = gradients[index.coerceIn(0, gradients.lastIndex)]

    /**
     * Градиент по умолчанию — детерминированно выводится из ключа (обычно id пользователя).
     * Используется, когда фото нет и пресет не выбран: у каждого человека свой цвет,
     * и он не меняется от запуска к запуску.
     */
    fun gradientForKey(key: String): List<Color> {
        val h = key.fold(0) { acc, c -> acc * 31 + c.code }
        val idx = ((h % gradients.size) + gradients.size) % gradients.size
        return gradients[idx]
    }

    /** Инициалы: одна буква из имени или две, если указаны имя и фамилия. */
    fun initialsOf(name: String?): String {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return "?"
        val parts = n.split(" ", "_").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            else -> n.first().uppercaseChar().toString()
        }
    }
}
