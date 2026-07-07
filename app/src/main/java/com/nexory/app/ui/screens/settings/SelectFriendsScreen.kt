package com.nexory.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexory.app.data.network.FriendDto
import com.nexory.app.data.network.NexoryApi
import com.nexory.app.ui.theme.NexoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectFriendsUiState(
    val friends:  List<FriendDto> = emptyList(),
    val selected: Set<String>     = emptySet(),
    val isLoading: Boolean        = false,
    val isSaved:   Boolean        = false,
)

@HiltViewModel
class SelectFriendsViewModel @Inject constructor(
    private val api: NexoryApi,
) : ViewModel() {
    private val _state = MutableStateFlow(SelectFriendsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val friends = api.getFriends()["friends"] ?: emptyList()
                val allowed = api.getAllowedFriends()["ids"]?.toSet() ?: emptySet()
                _state.update { it.copy(friends = friends, selected = allowed, isLoading = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggle(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun save() {
        viewModelScope.launch {
            try {
                api.setAllowedFriends(mapOf("ids" to _state.value.selected.toList()))
                _state.update { it.copy(isSaved = true) }
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFriendsScreen(
    navController: NavController,
    viewModel: SelectFriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isSaved) { if (state.isSaved) navController.popBackStack() }

    Scaffold(
        containerColor = NexoryColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Кому виден профиль", color = NexoryColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = NexoryColors.TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) {
                        Text("Готово", color = NexoryColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoryColors.SurfaceDark),
            )
        }
    ) { padding ->
        if (state.friends.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Пока нет друзей", color = NexoryColors.TextSecondary)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.friends, key = { it.id }) { friend ->
                val checked = friend.id in state.selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoryColors.SurfaceDark)
                        .clickable { viewModel.toggle(friend.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = friend.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(NexoryColors.SurfaceMid),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(friend.username, color = NexoryColors.TextPrimary, modifier = Modifier.weight(1f))
                    Icon(
                        if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null, tint = if (checked) NexoryColors.PrimaryBlue else NexoryColors.TextSecondary,
                    )
                }
            }
        }
    }
}

private fun RoundedCornerShape(dp: androidx.compose.ui.unit.Dp) = androidx.compose.foundation.shape.RoundedCornerShape(dp)
