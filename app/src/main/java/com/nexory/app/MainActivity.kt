package com.nexory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.nexory.app.data.local.SettingsManager
import com.nexory.app.data.local.ThemeMode
import com.nexory.app.data.local.TokenManager
import com.nexory.app.navigation.AppNavHost
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
                // null = ещё загружается, true/false = состояние авторизации.
                val isLoggedIn: Boolean? by remember {
                    tokenManager.isLoggedIn().map { it as Boolean? }
                }.collectAsState(initial = null)

                // Показывали ли онбординг (для новых пользователей)
                val onboardingDone: Boolean? by remember {
                    settingsManager.onboardingDone.map { it as Boolean? }
                }.collectAsState(initial = null)

                val navController = rememberNavController()

                AppNavHost(
                    navController  = navController,
                    isLoggedIn     = isLoggedIn,
                    onboardingDone = onboardingDone,
                )
            }
        }
    }
}
