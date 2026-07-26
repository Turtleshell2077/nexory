package com.nexory.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Готовые аватары, которые пользователь выбирает вместо фотографии.
 *
 * Формат хранения — строка в поле `avatar_url`:
 *   `preset:<style>:<variant>`   например `preset:rings:2`
 *   `preset:<N>`                 старый формат, поддерживается для совместимости
 *                                (у уже выбравших пользователей он сохранён)
 *
 * Благодаря такому хранению не понадобилось ни новое поле в API, ни миграция БД:
 * все экраны уже передают avatar_url в [UserAvatar], а он сам решает, рисовать
 * картинку или сгенерированный аватар.
 *
 * Идентификаторы стилей и порядок вариантов менять нельзя — они уже у пользователей.
 */
object AvatarPresets {

    private const val PREFIX = "preset:"

    /** Стиль аватара: базовый градиент плюс пять узорных. */
    enum class Style(val id: String, val title: String) {
        GRADIENT("grad",   "Градиент"),
        RINGS   ("rings",  "Кольца"),
        DOTS    ("dots",   "Точки"),
        STRIPES ("stripes","Полосы"),
        BLOCKS  ("blocks", "Блоки"),
        BURST   ("burst",  "Лучи"),
    }

    /** Выбранный аватар: стиль + номер цветового варианта внутри стиля. */
    data class Selection(val style: Style, val variant: Int)

    /**
     * Цветовые пары для вариантов. Один и тот же набор используется всеми стилями —
     * так палитра приложения остаётся цельной, а различаются аватары рисунком.
     */
    val palettes: List<List<Color>> = listOf(
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

    val variantCount: Int get() = palettes.size

    /** Строка для сохранения в avatar_url. */
    fun toUrl(style: Style, variant: Int): String =
        "$PREFIX${style.id}:${variant.coerceIn(0, palettes.lastIndex)}"

    /** Это выбранный аватар, а не ссылка на файл? */
    fun isPreset(url: String?): Boolean = url != null && url.startsWith(PREFIX)

    /**
     * Разбирает avatar_url в [Selection].
     * Понимает и новый формат `preset:style:variant`, и старый `preset:N`
     * (он трактуется как градиент с этим номером варианта).
     * Некорректные значения приводятся к валидным, чтобы старые данные
     * не ломали отрисовку.
     */
    fun parse(url: String?): Selection? {
        if (!isPreset(url)) return null
        val body = url!!.removePrefix(PREFIX)
        val parts = body.split(":")
        return if (parts.size >= 2) {
            val style = Style.entries.firstOrNull { it.id == parts[0] } ?: Style.GRADIENT
            val variant = parts[1].toIntOrNull() ?: 0
            Selection(style, variant.coerceIn(0, palettes.lastIndex))
        } else {
            // Старый формат: только номер варианта, стиль — градиент
            val variant = parts[0].toIntOrNull() ?: 0
            Selection(Style.GRADIENT, variant.coerceIn(0, palettes.lastIndex))
        }
    }

    fun paletteAt(variant: Int): List<Color> = palettes[variant.coerceIn(0, palettes.lastIndex)]

    /**
     * Аватар по умолчанию, когда ни фото, ни выбранного варианта нет:
     * цвет выводится детерминированно из ключа (обычно id пользователя),
     * поэтому у каждого он свой и не меняется от запуска к запуску.
     */
    fun defaultSelectionForKey(key: String): Selection {
        val h = key.fold(0) { acc, c -> acc * 31 + c.code }
        val idx = ((h % palettes.size) + palettes.size) % palettes.size
        return Selection(Style.GRADIENT, idx)
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
