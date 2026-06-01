package com.jiny.finalalarm.ui.inbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.UnlockRequestDto
import com.jiny.finalalarm.data.api.UnlockRequestStatus
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxListVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow<List<UnlockRequestDto>>(emptyList())
    val state = _state.asStateFlow()

    fun load(teamId: String) = viewModelScope.launch {
        _state.value = runCatching {
            api.inbox(teamId, UnlockRequestStatus.PENDING)
        }.getOrDefault(emptyList())
    }
}

@Composable
fun InboxListScreen(nav: NavController, teamId: String, vm: InboxListVm = hiltViewModel()) {
    LaunchedEffect(teamId) { vm.load(teamId) }
    val items by vm.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("잠금해제 요청", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = FaSpacing.screen),
        ) {
            if (items.isEmpty()) {
                item { EmptyState("대기 중인 요청이 없어요") }
            } else {
                items(items) { r ->
                    ListRow(
                        headline = "요청자 ${r.requesterId.take(8)}",
                        supporting = "만료 ${r.expiresAt.substringBefore('.').replace('T', ' ')}",
                        onClick = { nav.navigate(Routes.UNLOCK_DETAIL.replace("{id}", r.id)) },
                    )
                }
            }
        }
    }
}
