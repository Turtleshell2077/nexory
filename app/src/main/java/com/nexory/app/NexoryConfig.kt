package com.nexory.app

/**
 * Единая точка конфигурации адресов сервера.
 *
 * Раньше адрес был захардкожен в трёх местах (AppModule, ChatWebSocketManager) и легко
 * разъезжался. Теперь всё выводится из [SERVER_ORIGIN].
 *
 * ⚠️ ПЕРЕД РЕЛИЗОМ В GOOGLE PLAY: перевести сервер на HTTPS и заменить схему на https/wss.
 * Google Play в разделе Data Safety требует заявить шифрование данных при передаче;
 * обычный http этому не соответствует и является поводом для отклонения.
 * После перехода на домен с сертификатом достаточно поменять только эту строку
 * и убрать cleartext-исключение из res/xml/network_security_config.xml.
 */
object NexoryConfig {

    /**
     * Схема + хост + порт бэкенда, без завершающего слэша.
     * Значение приходит из buildConfigField в app/build.gradle.kts — так его можно
     * задавать отдельно для debug и release, не правя код.
     */
    val SERVER_ORIGIN: String = BuildConfig.SERVER_ORIGIN

    /** Базовый URL REST API для Retrofit (со завершающим слэшем — требование Retrofit). */
    val API_BASE_URL: String = "$SERVER_ORIGIN/api/v1/"

    /** WebSocket-эндпоинт чатов. Схема выводится из SERVER_ORIGIN: http→ws, https→wss. */
    val WS_URL: String = SERVER_ORIGIN.replaceFirst("http", "ws") + "/ws"

    /** Политика конфиденциальности — обязательна для модерации RuStore и Google Play. */
    val PRIVACY_URL: String = "$SERVER_ORIGIN/legal/privacy"

    /** Пользовательское соглашение. */
    val TERMS_URL: String = "$SERVER_ORIGIN/legal/terms"

    /** Страница удаления аккаунта — её URL указывается в Google Play Console. */
    val DELETE_ACCOUNT_URL: String = "$SERVER_ORIGIN/legal/delete-account"
}
