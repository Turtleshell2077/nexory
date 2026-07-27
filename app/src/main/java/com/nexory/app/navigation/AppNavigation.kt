package com.nexory.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexory.app.ui.screens.auth.LoginScreen
import com.nexory.app.ui.screens.auth.RegisterScreen
import com.nexory.app.ui.screens.chat.ArchiveScreen
import com.nexory.app.ui.screens.chat.ChatDetailScreen
import com.nexory.app.ui.screens.chat.ChatInfoScreen
import com.nexory.app.ui.screens.chat.ChatsScreen
import com.nexory.app.ui.screens.chat.NewChatScreen
import com.nexory.app.ui.screens.events.CreateEventScreen
import com.nexory.app.ui.screens.events.EventDetailScreen
import com.nexory.app.ui.screens.feed.FeedScreen
import com.nexory.app.ui.screens.friends.FriendsScreen
import com.nexory.app.ui.screens.profile.EditProfileScreen
import com.nexory.app.ui.screens.profile.ProfileScreen
import com.nexory.app.ui.screens.profile.UserProfileScreen
import com.nexory.app.ui.screens.settings.SelectFriendsScreen
import com.nexory.app.ui.screens.settings.SettingsScreen
import com.nexory.app.ui.screens.support.SupportScreen
import com.nexory.app.ui.theme.NexoryColors

sealed class Screen(val route: String) {
    object Login       : Screen("login")
    object Register    : Screen("register")
    object Feed        : Screen("feed")
    object Chats       : Screen("chats")
    object Friends     : Screen("friends")
    object Profile     : Screen("profile")
    object Support     : Screen("support")
    object Settings    : Screen("settings")
    object NewChat     : Screen("new_chat")
    object Archive     : Screen("archive")
    object SelectFriends : Screen("select_friends")
    object Development : Screen("development")
    object EditProfile : Screen("edit_profile")
    object CreateEvent : Screen("create_event")
    object EditEvent   : Screen("edit_event/{eventId}") {
        fun route(id: String) = "edit_event/$id"
    }

    object EventDetail : Screen("event/{eventId}") {
        fun route(id: String) = "event/$id"
    }
    object ChatDetail : Screen("chat/{chatId}") {
        fun route(id: String) = "chat/$id"
    }
    object ChatInfo : Screen("chat_info/{chatId}") {
        fun route(id: String) = "chat_info/$id"
    }
    object UserProfile : Screen("user/{userId}") {
        fun route(id: String) = "user/$id"
    }
    object Onboarding : Screen("onboarding")
    object VerifyEmail : Screen("verify_email")
    /** Согласие с политикой конфиденциальности — обязательно ДО входа/регистрации (RuStore). */
    object Consent : Screen("consent")

}

// Экраны, доступные неавторизованному пользователю. Используется в редиректах ниже,
// чтобы не выкидывать пользователя с экрана согласия или онбординга на Login.
private val PUBLIC_ROUTES = setOf(
    Screen.Login.route,
    Screen.Register.route,
    Screen.Onboarding.route,
    Screen.Consent.route,
)

@Composable
fun AppNavHost(
    navController: NavHostController,
    isLoggedIn: Boolean?,
    onboardingDone: Boolean? = true,
    legalAccepted: Boolean? = true,
) {
    // null = DataStore ещё загружается — показываем тёмный экран без мигания
    if (isLoggedIn == null || onboardingDone == null || legalAccepted == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NexoryColors.DeepBlack),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = NexoryColors.PrimaryBlue)
        }
        return
    }

    NavHost(
        navController    = navController,
        // Стартовый экран: залогинен → лента; новый пользователь → онбординг;
        // не принял документы → согласие (требование RuStore); иначе → вход
        startDestination = when {
            isLoggedIn        -> Screen.Feed.route
            !onboardingDone   -> Screen.Onboarding.route
            !legalAccepted    -> Screen.Consent.route
            else              -> Screen.Login.route
        },
        // Плавные переходы вместо резкой смены (без чёрных вспышек)
        enterTransition     = { fadeIn(animationSpec = tween(180)) },
        exitTransition      = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition  = { fadeIn(animationSpec = tween(180)) },
        popExitTransition   = { fadeOut(animationSpec = tween(180)) },
    ) {

        // ---- Onboarding ----
        composable(Screen.Onboarding.route) {
            com.nexory.app.ui.screens.onboarding.OnboardingScreen(navController = navController)
        }

        // ---- Согласие с документами (до входа/регистрации) ----
        composable(Screen.Consent.route) {
            com.nexory.app.ui.screens.legal.ConsentScreen(navController = navController)
        }

        // ---- Auth ----
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.VerifyEmail.route) {
            com.nexory.app.ui.screens.auth.VerifyEmailScreen(navController = navController)
        }

        // ---- Главные вкладки ----
        composable(Screen.Feed.route) {
            FeedScreen(navController = navController)
        }
        composable(Screen.Chats.route) {
            ChatsScreen(navController = navController)
        }
        composable(Screen.Friends.route) {
            FriendsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        // ---- Детальные экраны ----
        composable(
            route     = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            EventDetailScreen(navController = navController, eventId = eventId)
        }

        composable(
            route     = Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatDetailScreen(navController = navController, chatId = chatId)
        }

        composable(
            route     = Screen.ChatInfo.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatInfoScreen(navController = navController, chatId = chatId)
        }

        composable(
            route     = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserProfileScreen(navController = navController, userId = userId)
        }

        // ---- Прочие ----
        composable(Screen.CreateEvent.route) {
            CreateEventScreen(navController = navController)
        }
        composable(
            route     = Screen.EditEvent.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            CreateEventScreen(navController = navController, eventId = eventId)
        }
        composable(Screen.Support.route) {
            SupportScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.NewChat.route) {
            NewChatScreen(navController = navController)
        }
        composable(Screen.Archive.route) {
            ArchiveScreen(navController = navController)
        }
        composable(Screen.SelectFriends.route) {
            SelectFriendsScreen(navController = navController)
        }
        composable(Screen.Development.route) {
            com.nexory.app.ui.screens.development.DevelopmentScreen(navController = navController)
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }

        // Экраны выбора оформления аватара убраны: без фотографии пользователь
        // получает градиент, подобранный по его id, — выбирать нечего.
    }

    // Централизованное управление навигацией:
    // - при входе → Feed (очищаем весь стек, Login не остаётся снизу)
    // - при выходе → Login (очищаем весь стек)
    // - Нажатие Back на Feed завершает Activity т.к. стек пуст — это правильно
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentDestination?.route
        val onPublicScreen = currentRoute == null || currentRoute in PUBLIC_ROUTES
        when {
            isLoggedIn && onPublicScreen -> {
                navController.navigate(Screen.Feed.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            // При выходе ведём на согласие, если оно ещё не дано, иначе на вход
            !isLoggedIn && !onPublicScreen -> {
                val target = if (legalAccepted) Screen.Login.route else Screen.Consent.route
                navController.navigate(target) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}
