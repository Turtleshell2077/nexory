package com.nexory.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Шаблоны аватара-заглушки, которые пользователь выбирает вместо фотографии.
 *
 * Формат хранения — строка в поле `avatar_url`:
 *   `preset:<style>:<variant>`   например `preset:duotone:5`
 *   `preset:<N>`                 самый первый формат, трактуется как градиент
 *
 * Такое хранение не потребовало ни нового поля в API, ни миграции БД: все экраны
 * уже передают avatar_url в [UserAvatar], а он решает, рисовать фото или шаблон.
 *
 * Почему выбраны именно эти пять шаблонов: каждый даёт свой визуальный «характер»
 * и при этом читается и как иконка 40 dp в списке, и как аватар 100 dp в профиле.
 * Все построены на одной палитре, поэтому в приложении они смотрятся как одна
 * семья, а не как случайный набор.
 *
 * Идентификаторы стилей менять нельзя — они уже сохранены у пользователей.
 */
object AvatarPresets {

    private const val PREFIX = "preset:"

    enum class Style(val id: String, val title: String, val description: String) {
        /** Плавный переход трёх оттенков — спокойный вариант «по умолчанию». */
        GRADIENT("grad", "Градиент", "Плавный переход цветов"),

        /** Крупные инициалы на диагональном двухтоновом фоне — глубже плоской заливки. */
        DUOTONE("duotone", "Дуотон", "Крупные инициалы на двух оттенках"),

        /** Абстрактные фигуры, расставленные детерминированно по id пользователя. */
        GEOMETRIC("geo", "Геометрия", "Абстрактный узор, уникальный для вас"),

        /** Мягкие перетекающие формы — «органика». */
        WAVES("waves", "Волны", "Мягкие перетекающие формы"),

        /** Тонкая сетка поверх однотонного фона — фактура без шума. */
        TEXTURE("texture", "Текстура", "Тонкий узор на ровном фоне"),
    }

    data class Selection(val style: Style, val variant: Int)

    /**
     * Двенадцать палитр. Каждая — три опорных цвета: тёмный, основной и светлый.
     * Три точки вместо двух дают градиенту глубину, а остальным шаблонам —
     * достаточный контраст между фоном и рисунком.
     * Все проверены на читаемость белых инициалов поверх.
     */
    val palettes: List<List<Color>> = listOf(
        listOf(Color(0xFF3A1C71), Color(0xFF5B62F0), Color(0xFF8AB6F9)), // индиго
        listOf(Color(0xFF8E1F4B), Color(0xFFEE5A9E), Color(0xFFFFA9C9)), // фуксия
        listOf(Color(0xFF0B4F4A), Color(0xFF11998E), Color(0xFF57E8B0)), // изумруд
        listOf(Color(0xFF8A4B08), Color(0xFFF7971E), Color(0xFFFFD86F)), // янтарь
        listOf(Color(0xFF2B1055), Color(0xFF7B4FE0), Color(0xFFB79CFF)), // аметист
        listOf(Color(0xFF8C2F39), Color(0xFFFF6A88), Color(0xFFFFB3C1)), // коралл
        listOf(Color(0xFF003B73), Color(0xFF0080C6), Color(0xFF66C6FF)), // океан
        listOf(Color(0xFF5B1865), Color(0xFFB93FBF), Color(0xFFF39BFF)), // орхидея
        listOf(Color(0xFF0F5132), Color(0xFF2FA35C), Color(0xFF8FE3AC)), // мох
        listOf(Color(0xFF7A3B1F), Color(0xFFE0703C), Color(0xFFFFB08A)), // терракота
        listOf(Color(0xFF0D3B4C), Color(0xFF1C8C9E), Color(0xFF7FD9E3)), // бирюза
        listOf(Color(0xFF37474F), Color(0xFF62808F), Color(0xFFAFC4CE)), // графит
    )

    val variantCount: Int get() = palettes.size

    fun toUrl(style: Style, variant: Int): String =
        "$PREFIX${style.id}:${variant.coerceIn(0, palettes.lastIndex)}"

    fun isPreset(url: String?): Boolean = url != null && url.startsWith(PREFIX)

    /**
     * Разбирает avatar_url. Неизвестный стиль (например, из более старой версии
     * приложения) не ломает отрисовку — откатываемся к градиенту.
     */
    fun parse(url: String?): Selection? {
        if (!isPreset(url)) return null
        val parts = url!!.removePrefix(PREFIX).split(":")
        return if (parts.size >= 2) {
            val style = Style.entries.firstOrNull { it.id == parts[0] } ?: Style.GRADIENT
            Selection(style, (parts[1].toIntOrNull() ?: 0).coerceIn(0, palettes.lastIndex))
        } else {
            Selection(Style.GRADIENT, (parts[0].toIntOrNull() ?: 0).coerceIn(0, palettes.lastIndex))
        }
    }

    fun paletteAt(variant: Int): List<Color> = palettes[variant.coerceIn(0, palettes.lastIndex)]

    /**
     * Аватар по умолчанию, когда ни фото, ни выбранного шаблона нет.
     * Цвет выводится из ключа (обычно id пользователя): выглядит «случайным»,
     * но у конкретного человека всегда один и тот же.
     */
    fun defaultSelectionForKey(key: String): Selection =
        Selection(Style.GRADIENT, hashOf(key) % palettes.size)

    /** Стабильный неотрицательный хэш строки — используется и для узоров. */
    fun hashOf(key: String): Int {
        val h = key.fold(0) { acc, c -> acc * 31 + c.code }
        return if (h < 0) -h else h
    }

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
