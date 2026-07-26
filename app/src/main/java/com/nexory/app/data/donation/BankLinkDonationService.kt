package com.nexory.app.data.donation

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nexory.app.data.network.ConnectivityObserver
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.ui.components.openExternalUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация пожертвований через платёжную ссылку банка (Т-банк).
 *
 * Как это работает и почему так:
 *  - Ссылка НЕ зашита в APK, а приходит с бэкенда (`GET /api/v1/config`, переменная
 *    окружения `DONATION_URL`). Так её можно сменить перезапуском сервера, не выпуская
 *    новую версию в сторе и не дожидаясь модерации.
 *  - Ссылку получает владелец вручную в приложении Т-банка: «Переводы» → «Ссылка на
 *    перевод», сумма открытая. Перевод идёт по СБП с карты любого банка, а номер
 *    телефона получателя плательщику не показывается — реквизиты живут на стороне банка.
 *  - Открываем во внешнем браузере через ACTION_VIEW (не WebView): платёжную страницу
 *    банка нельзя показывать внутри стороннего приложения, это и небезопасно,
 *    и ломает работу банковских редиректов.
 *  - Подтверждения оплаты приложение не получает (вебхука нет), поэтому UI обязан
 *    говорить о статусе нейтрально.
 *
 * Ограничения осознанные и отражены в [capabilities]. Полный флоу СБП с выбором банка
 * внутри приложения и подтверждением платежа появится в реализации через агрегатор —
 * достаточно будет подменить биндинг в `di/DonationModule.kt`.
 */
@Singleton
class BankLinkDonationService @Inject constructor(
    private val api: NexoryApi,
    private val connectivity: ConnectivityObserver,
    private val settings: com.nexory.app.data.local.SettingsManager,
) : DonationService {

    override val capabilities = DonationCapabilities(
        canChooseBankInApp = false, // выбор банка происходит на стороне СБП/банка
        canConfirmPayment  = false, // вебхука нет, статус платежа неизвестен
        needsAmountInApp   = false, // сумму вводит пользователь на странице банка
    )

    /**
     * Актуальная ссылка: сначала пробуем сервер, при неудаче — последнее сохранённое
     * значение. Кэш нужен, чтобы кнопка работала и при коротком сбое сети/сервера.
     */
    private suspend fun resolveDonationUrl(): String? {
        try {
            val config = api.getConfig()
            val url = config.donationUrl.trim()
            if (config.donationEnabled && url.isNotBlank()) {
                settings.setDonationUrl(url)
                return url
            }
            // Сервер явно сообщил, что приём переводов выключен — чистим кэш,
            // иначе кнопка продолжала бы вести на отозванную ссылку.
            settings.setDonationUrl("")
            return null
        } catch (_: Exception) {
            return settings.getDonationUrl()?.takeIf { it.isNotBlank() }
        }
    }

    override suspend fun start(context: Context, amountRubles: Int?): DonationResult {
        // Нет сети — ни конфиг не получим, ни страница банка не загрузится
        if (!connectivity.isOnline()) {
            return DonationResult.Failed(DonationError.NO_NETWORK)
        }

        val url = resolveDonationUrl()
        if (url.isNullOrBlank()) {
            return DonationResult.Failed(DonationError.NOT_CONFIGURED)
        }

        val opened = openExternalUrl(
            context = context,
            url = url,
            errorMessage = "Не удалось открыть страницу оплаты",
        )

        return if (opened) DonationResult.OpenedExternally
        else DonationResult.Failed(DonationError.CANNOT_OPEN)
    }
}
