package com.jiny.finalalarm.ui.history

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
import com.jiny.finalalarm.data.api.AlarmEventDto
import com.jiny.finalalarm.data.api.AlarmEventState
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow<List<AlarmEventDto>>(emptyList())
    val state = _state.asStateFlow()
    var loading by mutableStateOf(false)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        loading = true
        _state.value = runCatching { api.history(limit = 100) }.getOrDefault(emptyList())
        loading = false
    }
}

@Composable
fun HistoryScreen(nav: NavController, vm: HistoryVm = hiltViewModel()) {
    com.jiny.finalalarm.ui.components.OnResume { vm.refresh() }
    val events by vm.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("히스토리", style = MaterialTheme.typography.titleLarge) },
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
            contentPadding = PaddingValues(bottom = FaSpacing.xxl),
        ) {
            if (events.isEmpty() && !vm.loading) {
                item { EmptyState("기록이 없습니다") }
            } else {
                items(events) { e ->
                    val when_ = e.triggeredAt.substringBefore('.').replace('T', ' ')
                    val state = when (e.state) {
                        AlarmEventState.DISMISSED -> "해제"
                        AlarmEventState.EXPIRED -> "만료"
                        AlarmEventState.SNOOZED -> "스누즈"
                        AlarmEventState.UNLOCK_REQUESTED -> "요청 중"
                        AlarmEventState.UNLOCK_APPROVED -> "승인됨"
                        AlarmEventState.RINGING -> "울리는 중"
                    }
                    val source = if (e.senderUserId != null) "팀원이 깨움" else "본인 알람"
                    ListRow(
                        headline = when_,
                        supporting = "$state · $source",
                    )
                }
            }
        }
    }
}
