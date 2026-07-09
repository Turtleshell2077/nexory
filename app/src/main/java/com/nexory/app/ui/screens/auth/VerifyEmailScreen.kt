package com.nexory.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.ui.components.nexoryTextFieldColors
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.ExperimentalMaterial3Api

data class VerifyEmailUiState(
    val loading: Boolean = false,
    val error:   String? = null,
    val message: String? = null,
    val verified: Boolean = false,
)

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {
    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state = _state.asStateFlow()

    init { resend() }   // отправляем свежий код при открытии

    fun verify(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                api.verifyEmail(mapOf("code" to code))
                _state.update { it.copy(loading = false, verified = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Неверный или просроченный код") }
            }
        }
    }

    fun resend() {
        viewModelScope.launch {
            try {
                api.resendVerification()
                _state.update { it.copy(message = "Код отправлен на вашу почту") }
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    navController: NavController,
    viewModel: VerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state.verified) { if (state.verified) navController.popBackStack() }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Подтверждение почты", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NexoryColors.GradientStart, NexoryColors.GradientEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MarkEmailRead, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Введите код из письма", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NexoryColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Мы отправили 6-значный код на вашу почту. Введите его ниже, чтобы подтвердить аккаунт.",
                fontSize = 14.sp, color = NexoryColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Код из письма", color = NexoryColors.TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = nexoryTextFieldColors(),
            )

            state.message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = NexoryColors.PrimaryBlue, fontSize = 13.sp) }
            state.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = NexoryColors.Error, fontSize = 13.sp) }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.verify(code) },
                enabled = code.length >= 4 && !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
            ) {
                if (state.loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Подтвердить", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.resend() }) {
                Text("Отправить код заново", color = NexoryColors.PrimaryBlue)
            }
        }
    }
}
