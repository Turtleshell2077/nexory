package com.nexory.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.theme.NexoryColors
import com.nexory.app.ui.components.nexoryTextFieldColors

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Следим за логином — если успешен, NavHost сам уберёт этот экран
    // через isLoggedIn Flow. LaunchedEffect нужен для navigate-вызова.
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(Screen.Feed.route) {
                // Убираем Login из back stack — кнопка назад не вернёт на Login
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoryColors.DeepBlack)
    ) {
        // Декоративный градиентный блок сверху
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            NexoryColors.DeepBlue.copy(alpha = 0.5f),
                            NexoryColors.DeepBlack,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // Логотип / название приложения
            Text(
                text       = "Nexory",
                fontSize   = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                style      = LocalTextStyle.current.copy(
                    brush = Brush.linearGradient(
                        listOf(NexoryColors.PrimaryBlue, NexoryColors.LightViolet)
                    )
                )
            )
            Text(
                text     = "Находи мероприятия. Встречай людей.",
                fontSize = 14.sp,
                color    = NexoryColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 48.dp),
            )

            // Email
            OutlinedTextField(
                value           = email,
                onValueChange   = { email = it },
                modifier        = Modifier.fillMaxWidth(),
                label           = { Text("Email") },
                leadingIcon     = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine      = true,
                shape           = RoundedCornerShape(14.dp),
                colors          = nexoryTextFieldColors(),
            )
            Spacer(Modifier.height(14.dp))

            // Пароль
            OutlinedTextField(
                value         = password,
                onValueChange = { password = it },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Пароль") },
                leadingIcon   = { Icon(Icons.Default.Lock, null) },
                trailingIcon  = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            if (showPass) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = NexoryColors.TextSecondary,
                        )
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine      = true,
                shape           = RoundedCornerShape(14.dp),
                colors          = nexoryTextFieldColors(),
            )

            // Сообщение об ошибке
            uiState.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = NexoryColors.Error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(28.dp))

            // Кнопка входа с градиентом
            Button(
                onClick  = { viewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled  = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd)
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color     = Color.White,
                            modifier  = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Войти",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Переход на регистрацию
            TextButton(
                onClick = { navController.navigate(Screen.Register.route) }
            ) {
                Text("Нет аккаунта? ", color = NexoryColors.TextSecondary, fontSize = 14.sp)
                Text(
                    "Зарегистрироваться",
                    color      = NexoryColors.PrimaryBlue,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}