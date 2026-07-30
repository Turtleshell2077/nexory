package com.nexory.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Палитра приложения.
 *
 * ВСЕ цвета здесь реактивные (`mutableStateOf`) и переключаются в [apply].
 * Раньше брендовые цвета были константами, и светлая тема получалась простой
 * инверсией тёмной: те же неоновые оттенки, рассчитанные на чёрный фон,
 * выкладывались на белый и выглядели кислотно.
 *
 * Теперь у тем разные характеры:
 *  - тёмная — ночная, светящаяся: яркий индиго на почти чёрном;
 *  - светлая — дневная, воздушная: приглушённый лавандовый фон, белые карточки
 *    с тонкой рамкой и более ГЛУБОКИЕ, насыщенные акценты (светлый оттенок на
 *    белом просто не читается — нужен темнее, а не ярче).
 *
 * ⚠️ ВАЖНО: не читайте эти поля в top-level `val` — значение «запечётся» на
 * этапе загрузки класса и перестанет реагировать на смену темы. Ровно на этом
 * когда-то сломалась типографика: весь текст оставался почти белым.
 * Читать только внутри @Composable (или функции, вызываемой из композиции).
 */
object NexoryColors {

    /** Текущая тема — нужна там, где одного цвета мало (например, фон чата). */
    var isDark: Boolean by mutableStateOf(true); private set

    // ---- Фон и поверхности ----
    /** Фон экрана. Историческое имя: в тёмной теме он почти чёрный. */
    var DeepBlack:     Color by mutableStateOf(Color(0xFF0A0A12)); private set
    /** Карточки, панели, шапки. */
    var SurfaceDark:   Color by mutableStateOf(Color(0xFF12121F)); private set
    /** Поля ввода, неактивные чипы, разделители. */
    var SurfaceMid:    Color by mutableStateOf(Color(0xFF1E1E30)); private set
    /**
     * Рамка карточек. В светлой теме белая карточка на светлом фоне без границы
     * расплывается — тонкая лавандовая линия возвращает ей форму.
     * В тёмной теме карточка и так отделяется от фона: рамка прозрачная.
     */
    var CardBorder:    Color by mutableStateOf(Color.Transparent); private set

    // ---- Текст ----
    var TextPrimary:   Color by mutableStateOf(Color(0xFFF0F0F8)); private set
    var TextSecondary: Color by mutableStateOf(Color(0xFF8888AA)); private set

    // ---- Брендовые ----
    /** Основной акцент. Имя историческое — цвет индиговый, не синий. */
    var PrimaryBlue:   Color by mutableStateOf(Color(0xFF5B62F0)); private set
    /** Плотная подложка: свои сообщения, активная вкладка ленты. */
    var DeepBlue:      Color by mutableStateOf(Color(0xFF272B63)); private set
    var Violet:        Color by mutableStateOf(Color(0xFF7B4FE0)); private set
    var LightViolet:   Color by mutableStateOf(Color(0xFFAA80FF)); private set
    var GradientStart: Color by mutableStateOf(Color(0xFF5B62F0)); private set
    var GradientEnd:   Color by mutableStateOf(Color(0xFF8B49E8)); private set
    var Error:         Color by mutableStateOf(Color(0xFFE25A5A)); private set

    /**
     * Сиреневый для ТЕКСТА поверх фона (категория мероприятия, статус, имя
     * отправителя). Отдельно от [LightViolet]: светлый сиреневый хорош на
     * тёмном, но на белом сливается — там нужен глубокий.
     */
    var AccentText:    Color by mutableStateOf(Color(0xFFAA80FF)); private set

    /**
     * Тёплый второй акцент — единственный не-фиолетовый цвет в палитре.
     * Используется точечно, там где речь о деньгах (цена платного мероприятия):
     * на фоне сплошного индиго тёплое пятно сразу цепляет взгляд.
     */
    var Accent2:       Color by mutableStateOf(Color(0xFFFFB067)); private set

    fun apply(dark: Boolean) {
        isDark = dark
        if (dark) {
            // Ночная тема: глубокий фон, светящиеся акценты
            DeepBlack     = Color(0xFF0A0A12)
            SurfaceDark   = Color(0xFF12121F)
            SurfaceMid    = Color(0xFF1E1E30)
            CardBorder    = Color.Transparent
            TextPrimary   = Color(0xFFF0F0F8)
            TextSecondary = Color(0xFF8888AA)

            // ПРО КОНТРАСТ АКЦЕНТА В ТЁМНОЙ ТЕМЕ.
            // Этот цвет работает в двух ролях сразу: им пишут текст ПО тёмному
            // фону и им же заливают кнопки ПОД белый текст. Требования
            // противоположны и несовместимы арифметически: чтобы текст на фоне
            // #0A0A12 дал 4.5, яркость цвета должна быть ≥ 0.214, а чтобы белый
            // на этой заливке дал 4.5 — ≤ 0.183. Одним цветом обе нормы не взять.
            // Выбран баланс в пользу ЗАЛИВКИ (белый на кнопке — 4.72), потому что
            // крупных кнопок в интерфейсе больше, чем акцентных подписей.
            // Полное решение — развести роли на два цвета; см. ROADMAP.
            PrimaryBlue   = Color(0xFF5B62F0)
            DeepBlue      = Color(0xFF272B63)
            Violet        = Color(0xFF7B4FE0)
            LightViolet   = Color(0xFFAA80FF)
            GradientStart = Color(0xFF5B62F0)
            GradientEnd   = Color(0xFF8B49E8)
            Error         = Color(0xFFE25A5A)
            AccentText    = Color(0xFFAA80FF)
            Accent2       = Color(0xFFFFB067)
        } else {
            // Дневная тема: лавандовый воздух, белые карточки, глубокие акценты.
            //
            // Иерархия поверхностей выстроена в обе стороны от фона: карточки
            // СВЕТЛЕЕ его, поля ввода — ТЕМНЕЕ. Так и то и другое читается без
            // рамок и теней. Когда фон был почти белым (#F7F6FD), белая карточка
            // на нём просто растворялась.
            DeepBlack     = Color(0xFFF0EEFA)  // фон экрана — заметный лавандовый
            SurfaceDark   = Color(0xFFFFFFFF)  // карточки и панели — светлее фона
            SurfaceMid    = Color(0xFFE7E4F6)  // поля ввода и чипы — темнее фона
            CardBorder    = Color(0xFFE2DEF3)  // тонкая рамка карточек
            TextPrimary   = Color(0xFF221F3D)  // чернильный индиго вместо чёрного
            TextSecondary = Color(0xFF5C5880)  // темнее, чем просится «на глаз»:
                                               // на лавандовом фоне более светлый
                                               // оттенок не дотягивал до нормы контраста

            // Акценты ГЛУБЖЕ, чем в тёмной теме: на белом светлый оттенок
            // теряется, а насыщенный держит контраст и выглядит опрятно
            PrimaryBlue   = Color(0xFF5A46E8)
            DeepBlue      = Color(0xFF5A46E8)  // свои сообщения — фирменным цветом,
                                               // тёмно-синий на светлом фоне мрачен
            Violet        = Color(0xFF7B3FE4)
            LightViolet   = Color(0xFF9061F0)
            GradientStart = Color(0xFF5A46E8)
            GradientEnd   = Color(0xFF9B4DE0)
            Error         = Color(0xFFBE3B3B)
            AccentText    = Color(0xFF6D28D9)
            // Тёплый акцент на белом должен быть заметно глубже, чем на тёмном,
            // иначе мелкий текст цены выцветает
            Accent2       = Color(0xFF9E4A08)
        }
    }
}

private fun darkScheme() = darkColorScheme(
    primary        = NexoryColors.PrimaryBlue,
    secondary      = NexoryColors.Violet,
    tertiary       = NexoryColors.LightViolet,
    background     = NexoryColors.DeepBlack,
    surface        = NexoryColors.SurfaceDark,
    surfaceVariant = NexoryColors.SurfaceMid,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = NexoryColors.TextPrimary,
    onSurface      = NexoryColors.TextPrimary,
    outline        = NexoryColors.CardBorder,
    error          = NexoryColors.Error,
)

private fun lightScheme() = lightColorScheme(
    primary        = NexoryColors.PrimaryBlue,
    secondary      = NexoryColors.Violet,
    tertiary       = NexoryColors.LightViolet,
    background     = NexoryColors.DeepBlack,
    surface        = NexoryColors.SurfaceDark,
    surfaceVariant = NexoryColors.SurfaceMid,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = NexoryColors.TextPrimary,
    onSurface      = NexoryColors.TextPrimary,
    outline        = NexoryColors.CardBorder,
    error          = NexoryColors.Error,
)

@Composable
fun NexoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Применяем палитру в начале композиции — родитель пишет до того, как дети читают.
    NexoryColors.apply(darkTheme)

    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme() else lightScheme(),
        typography  = NexoryTypography,
        content     = content,
    )
}
