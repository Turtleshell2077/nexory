package com.nexory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nexory.app.data.local.SettingsManager
import com.nexory.app.data.local.ThemeMode
import com.nexory.app.data.local.TokenManager
import androidx.compose.runtime.saveable.rememberSaveable
import com.nexory.app.navigation.AppNavHost
import com.nexory.app.ui.screens.security.PinLockScreen
import com.nexory.app.ui.screens.security.PinMode
import com.nexory.app.ui.theme.NexoryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Тема: системная / светлая / тёмная
            val themeMode by settingsManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK  -> true
                ThemeMode.SYSTEM -> systemDark
            }

            NexoryTheme(darkTheme = darkTheme) {
                // Фоновая поверхность под всем содержимым — убирает чёрные вспышки
                // в моменты переходов между экранами.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // null = ещё загружается, true/false = состояние авторизации.
                    val isLoggedIn: Boolean? by remember {
                        tokenManager.isLoggedIn().map { it as Boolean? }
                    }.collectAsState(initial = null)

                    // Показывали ли онбординг (для новых пользователей)
                    val onboardingDone: Boolean? by remember {
                        settingsManager.onboardingDone.map { it as Boolean? }
                    }.collectAsState(initial = null)

                    // PIN-код: если включён и пользователь залогинен — блокируем вход
                    // до правильного PIN. rememberSaveable → поворот экрана не сбрасывает разблокировку.
                    val pinEnabled by settingsManager.pinEnabled.collectAsState(initial = false)
                    var unlocked by rememberSaveable { mutableStateOf(false) }

                    val navController = rememberNavController()

                    if (isLoggedIn == true && pinEnabled && !unlocked) {
                        PinLockScreen(
                            mode = PinMode.ENTER,
                            onCheck = { settingsManager.checkPin(it) },
                            onSuccess = { unlocked = true },
                        )
                    } else {
                        AppNavHost(
                            navController  = navController,
                            isLoggedIn     = isLoggedIn,
                            onboardingDone = onboardingDone,
                        )
                    }
                }
            }
        }
    }
}
