package com.jiny.finalalarm.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.data.api.AlarmEventDto
import com.jiny.finalalarm.data.api.AlarmEventState
import com.jiny.finalalarm.data.api.FinalAlarmApi
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
    val events by vm.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("알람 히스토리") }) }) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            if (vm.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events) { e ->
                    val label = e.triggeredAt.substringBefore('.').replace('T', ' ')
                    val tag = when (e.state) {
                        AlarmEventState.DISMISSED -> "✅ 해제"
                        AlarmEventState.EXPIRED -> "⏰ 만료"
                        AlarmEventState.SNOOZED -> "💤 스누즈"
                        AlarmEventState.UNLOCK_REQUESTED -> "🔓 요청 중"
                        AlarmEventState.UNLOCK_APPROVED -> "🔓 승인됨"
                        AlarmEventState.RINGING -> "🔔 울리는 중"
                    }
                    val source = if (e.senderUserId != null) "팀원이 깨움" else "본인 알람"
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text("$tag · $source · ${e.snoozeCount}회 스누즈") },
                    )
                    HorizontalDivider()
                }
                if (events.isEmpty() && !vm.loading) item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("기록이 없습니다")
                    }
                }
            }
        }
    }
}
