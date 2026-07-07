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
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(Screen.Feed.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    var username by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var phone    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(NexoryColors.DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("Регистрация", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = NexoryColors.TextPrimary)
            }

            Spacer(Modifier.height(32.dp))

            // Поле: Имя пользователя
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя пользователя *") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoryTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email *") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoryTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))

            // Телефон (опционально)
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Телефон (необязательно)") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoryTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))

            // Пароль
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль *") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoryTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))

            // Подтверждение пароля
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Повтори пароль *") },
                leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                visualTransformation = PasswordVisualTransformation(),
                isError = confirm.isNotEmpty() && confirm != password,
                supportingText = if (confirm.isNotEmpty() && confirm != password) {{
                    Text("Пароли не совпадают", color = NexoryColors.Error)
                }} else null,
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoryTextFieldColors(),
            )
            Spacer(Modifier.height(8.dp))

            uiState.error?.let { Text(it, color = NexoryColors.Error, fontSize = 13.sp) }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password == confirm) viewModel.register(username, email, password, phone.takeIf { it.isNotBlank() })
                    else { /* показываем ошибку выше */ }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !uiState.isLoading && password == confirm,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isLoading)
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else
                        Text("Создать аккаунт", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}