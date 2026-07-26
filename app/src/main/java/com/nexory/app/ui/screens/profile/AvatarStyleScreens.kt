package com.nexory.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.navigation.Screen
import com.nexory.app.ui.components.AvatarPresets
import com.nexory.app.ui.components.GeneratedAvatar
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvatarStyleUiState(
    val userName: String? = null,
    val userId:   String? = null,
    val currentUrl: String? = null,
    val isSaving: Boolean = false,
    val isSaved:  Boolean = false,
    val error:    String? = null,
)

@HiltViewModel
class AvatarStyleViewModel @Inject constructor(
    private val api: NexoryApi,
    private val cache: com.nexory.app.data.local.OfflineCache,
) : ViewModel() {

    private val _state = MutableStateFlow(AvatarStyleUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Имя нужно для инициалов в превью, id — для узора геометрического шаблона
            val user = try { api.getMyProfile().user } catch (_: Exception) { cache.loadMyProfile() }
            _state.update {
                it.copy(
                    userName = user?.displayName?.takeIf { n -> n.isNotBlank() } ?: user?.username,
                    userId = user?.id,
                    currentUrl = user?.avatarUrl,
                )
            }
        }
    }

    /** Сохраняет выбранный шаблон и цвет как аватар пользователя. */
    fun apply(style: AvatarPresets.Style, variant: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val url = AvatarPresets.toUrl(style, variant)
                val response = api.updateProfile(mapOf("avatar_url" to url))
                response.user?.let { cache.saveMyProfile(it) }
                _state.update { it.copy(isSaving = false, isSaved = true, currentUrl = url) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = com.nexory.app.data.network.ApiError.message(e))
                }
            }
        }
    }
}

/**
 * Шаг 1 — выбор шаблона.
 *
 * Показываем крупные превью, чтобы отличия шаблонов были видны сразу, а не
 * угадывались по названию. Цвет на этом шаге не выбирается: для превью берётся
 * тот, что уже стоит у пользователя (или первый).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarStyleScreen(
    navController: NavController,
    viewModel: AvatarStyleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val current = AvatarPresets.parse(state.currentUrl)
    val initials = remember(state.userName) { AvatarPresets.initialsOf(state.userName) }
    val previewVariant = current?.variant ?: 0

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Стиль аватара", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Выберите стиль, а на следующем шаге — цвет",
                    color = NexoryColors.TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(AvatarPresets.Style.entries, key = { it.id }) { style ->
                val isCurrent = current?.style == style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isCurrent) NexoryColors.PrimaryBlue.copy(alpha = 0.12f)
                            else NexoryColors.SurfaceDark
                        )
                        .then(
                            if (isCurrent) Modifier.border(1.5.dp, NexoryColors.PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .clickable { navController.navigate(Screen.AvatarVariant.route(style.id)) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GeneratedAvatar(
                        selection = AvatarPresets.Selection(style, previewVariant),
                        initials = initials,
                        size = 60.dp,
                        seed = state.userId ?: initials,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(style.title, color = NexoryColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(style.description, color = NexoryColors.TextSecondary, fontSize = 12.sp)
                        if (isCurrent) {
                            Spacer(Modifier.height(2.dp))
                            Text("Сейчас выбран", color = NexoryColors.PrimaryBlue, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = NexoryColors.TextSecondary)
                }
            }
        }
    }
}

/**
 * Шаг 2 — выбор цвета внутри шаблона.
 *
 * Сверху — крупное превью, чтобы было видно, как аватар выглядит в профиле.
 * Ниже — сетка вариантов: их 12, все в одном стиле, отличаются только палитрой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarVariantScreen(
    navController: NavController,
    styleId: String,
    viewModel: AvatarStyleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val style = AvatarPresets.Style.entries.firstOrNull { it.id == styleId } ?: AvatarPresets.Style.GRADIENT
    val current = AvatarPresets.parse(state.currentUrl)
    val initials = remember(state.userName) { AvatarPresets.initialsOf(state.userName) }

    // Локальный выбор: применяем только по кнопке, чтобы можно было полистать варианты
    var selectedVariant by remember(current) {
        mutableStateOf(if (current?.style == style) current.variant else 0)
    }

    // После успешного сохранения возвращаемся сразу в профиль
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack(Screen.EditProfile.route, inclusive = false)
        }
    }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text(style.title, color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexoryColors.SurfaceDark)
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                state.error?.let {
                    Text(it, color = NexoryColors.Error, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = { viewModel.apply(style, selectedVariant) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexoryColors.PrimaryBlue),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Применить", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            // Крупное превью — так видно фактуру, которая теряется в маленькой иконке
            GeneratedAvatar(
                selection = AvatarPresets.Selection(style, selectedVariant),
                initials = initials,
                size = 120.dp,
                seed = state.userId ?: initials,
            )
            Spacer(Modifier.height(8.dp))
            Text(style.description, color = NexoryColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items((0 until AvatarPresets.variantCount).toList()) { variant ->
                    val selected = selectedVariant == variant
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (selected) Modifier.border(3.dp, NexoryColors.PrimaryBlue, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedVariant = variant },
                        contentAlignment = Alignment.Center,
                    ) {
                        GeneratedAvatar(
                            selection = AvatarPresets.Selection(style, variant),
                            initials = initials,
                            size = 60.dp,
                            seed = state.userId ?: initials,
                        )
                    }
                }
            }
        }
    }
}
