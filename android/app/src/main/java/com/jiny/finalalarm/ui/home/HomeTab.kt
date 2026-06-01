package com.jiny.finalalarm.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.data.api.AlarmDto
import com.jiny.finalalarm.data.api.AlarmEventDto
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.components.EmptyState
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.theme.FA
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class HomeUi(
    val upcoming: List<AlarmDto> = emptyList(),
    val active: List<AlarmEventDto> = emptyList(),
)

@HiltViewModel
class HomeVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow(HomeUi())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val alarms = runCatching { api.listAlarms(active = true) }.getOrDefault(emptyList())
        val active = runCatching { api.listActiveEvents() }.getOrDefault(emptyList())
        _state.value = HomeUi(upcoming = alarms, active = active)
    }
}

private fun greeting(): Pair<String, String> {
    val h = LocalTime.now().hour
    return when (h) {
        in 5..10 -> "🌅" to "좋은 아침"
        in 11..16 -> "☀️" to "안녕하세요"
        in 17..20 -> "🌇" to "수고했어요"
        else -> "🌙" to "오늘도 고생했어요"
    }
}

@Composable
fun HomeTab(nav: NavController, modifier: Modifier = Modifier, vm: HomeVm = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    val (emoji, greet) = greeting()
    val msg = when {
        ui.active.isNotEmpty() -> "지금 알람이 울리고 있어요"
        ui.upcoming.isEmpty() -> "아직 알람이 없네요. 추가해볼까요?"
        else -> "오늘은 ${ui.upcoming.size}개의 알람이 있어요"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FA.BgGradient)
            .padding(horizontal = FaSpacing.lg),
    ) {
        item {
            HelloHeader(emoji = emoji, title = greet, subtitle = msg)
        }

        if (ui.active.isNotEmpty()) {
            item { Section("지금 울리는 중") {} }
            items(ui.active) { e ->
                ListRow(
                    headline = "🔔 ${e.state}",
                    supporting = e.senderUserId?.let { "팀원이 깨우는 중" },
                )
            }
        }

        if (ui.upcoming.isNotEmpty()) {
            item { Section("오늘의 알람") {} }
            items(ui.upcoming) { a ->
                val icon = if (a.kind.name == "TEAM_APPROVAL") "👥" else "⏰"
                ListRow(
                    headline = "$icon  ${a.label}",
                    supporting = a.timeOfDay ?: a.oneShotAt ?: "—",
                )
            }
        } else if (ui.active.isEmpty()) {
            item { EmptyState(emoji = "🌱", text = "첫 알람을 추가해서\n친구들과 함께 일어나봐요") }
        }
    }
}
