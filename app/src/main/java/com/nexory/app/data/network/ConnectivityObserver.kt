package com.nexory.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Наблюдатель за состоянием сети.
 *
 * Используется в двух местах:
 *  - оффлайн-режим: показать баннер и отдать данные из локального кэша;
 *  - донат: не открывать платёжную страницу, если сети нет — иначе пользователь
 *    увидит пустой браузер и решит, что сломалось приложение.
 *
 * Требует разрешения ACCESS_NETWORK_STATE (объявлено в манифесте, опасным не считается
 * и отдельного запроса в рантайме не требует).
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /** Мгновенная проверка: есть ли интернет прямо сейчас. */
    fun isOnline(): Boolean {
        val cm = manager ?: return true // не смогли определить — не мешаем работе
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Поток состояния сети. Эмитит текущее значение сразу при подписке,
     * далее — при каждом изменении. distinctUntilChanged убирает дребезг:
     * система шлёт onCapabilitiesChanged довольно часто.
     */
    val isOnlineFlow: Flow<Boolean> = callbackFlow {
        val cm = manager
        if (cm == null) {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }

        trySend(isOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(isOnline()) }
            override fun onLost(network: Network) { trySend(isOnline()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(isOnline())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()
}
