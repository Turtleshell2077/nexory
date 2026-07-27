package com.nexory.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Градиентный аватар-заглушка для пользователей без фотографии.
 *
 * Решение намеренно простое: никакого выбора шаблонов и экранов настройки.
 * Палитра выводится детерминированно из id пользователя — аватар «случайный»
 * на вид, но у конкретного человека всегда один и тот же, и в списке друзей,
 * и в чатах, и в профиле.
 *
 * Формат в поле `avatar_url` не используется: аватар не хранится вовсе, он
 * вычисляется на лету. Строки вида `preset:...` от прежней версии с шаблонами
 * ещё могут встретиться в БД — [isLegacyPreset] позволяет их распознать и
 * отрисовать как обычный градиент, чтобы ни у кого не сломался аватар.
 */
object AvatarPresets {

    private const val LEGACY_PREFIX = "preset:"

    /**
     * Двенадцать палитр. Подбирались по трём правилам:
     *  - соседние оттенки одного цветового семейства, а не контрастная пара —
     *    так переход выглядит спокойным, без «кислоты»;
     *  - средняя светлота, чтобы белые инициалы читались поверх любой из них;
     *  - разные семейства между собой, чтобы два случайных пользователя
     *    редко получали похожие аватары.
     */
    val palettes: List<List<Color>> = listOf(
        listOf(Color(0xFF5B62F0), Color(0xFF8E7BFF)), // индиго
        listOf(Color(0xFF2E7BE0), Color(0xFF62B0F5)), // лазурь
        listOf(Color(0xFF11998E), Color(0xFF43C6AC)), // изумруд
        listOf(Color(0xFF2FA35C), Color(0xFF7DD68A)), // мох
        listOf(Color(0xFFE0703C), Color(0xFFF5A86B)), // терракота
        listOf(Color(0xFFD9584E), Color(0xFFF08A7E)), // кирпич
        listOf(Color(0xFFC2437E), Color(0xFFEE7BA8)), // фуксия
        listOf(Color(0xFF8B49E8), Color(0xFFB98BF2)), // аметист
        listOf(Color(0xFF1C8C9E), Color(0xFF5FC4D0)), // бирюза
        listOf(Color(0xFFD79A2B), Color(0xFFEFC46A)), // янтарь
        listOf(Color(0xFF5E6B8C), Color(0xFF94A2C0)), // сумеречный синий
        listOf(Color(0xFF4A7C6F), Color(0xFF86B3A5)), // шалфей
    )

    /** Осталась ли в БД строка от прежней версии с шаблонами. */
    fun isLegacyPreset(url: String?): Boolean = url != null && url.startsWith(LEGACY_PREFIX)

    /**
     * Это «настоящая» фотография, которую можно открыть на весь экран?
     * Для градиента открывать нечего — раньше попытка сделать это давала тёмный экран.
     */
    fun isRealPhoto(url: String?): Boolean = !url.isNullOrBlank() && !isLegacyPreset(url)

    /** Палитра по стабильному ключу (обычно id пользователя). */
    fun paletteForKey(key: String): List<Color> {
        val h = key.fold(0) { acc, c -> acc * 31 + c.code }
        val idx = ((h % palettes.size) + palettes.size) % palettes.size
        return palettes[idx]
    }

    /** Инициалы: одна буква или две, если указаны имя и фамилия. */
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
