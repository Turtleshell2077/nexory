package com.nexory.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors

@Composable
fun NexoryBottomBar(navController: NavController, currentRoute: String) {
    NavigationBar(containerColor = NexoryColors.SurfaceDark, tonalElevation = 0.dp) {
        val items = listOf(
            Triple(Screen.Feed.route,    Icons.Default.Home,   "Лента"),
            Triple(Screen.Chats.route,   Icons.Default.Chat,   "Чаты"),
            Triple(Screen.Friends.route, Icons.Default.People, "Друзья"),
            Triple(Screen.Profile.route, Icons.Default.Person, "Профиль"),
        )
        items.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick  = {
                    if (!selected) navController.navigate(route) {
                        popUpTo(Screen.Feed.route) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Icon(icon, contentDescription = label, tint = if (selected) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary) },
                label = { Text(label, fontSize = 11.sp, color = if (selected) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = NexoryColors.DeepBlue.copy(alpha = 0.2f)),
            )
        }
    }
}